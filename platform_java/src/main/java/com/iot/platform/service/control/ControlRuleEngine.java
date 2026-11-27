package com.iot.platform.service.control;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.ControlLog;
import com.iot.platform.model.ControlRule;
import com.iot.platform.repository.ControlLogRepository;
import com.iot.platform.repository.ControlRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 规则引擎：
 * <ul>
 *   <li>定时规则：每分钟扫描启用规则，命中当天执行时间触发（当天首次标记防重，按控制记录查重）；</li>
 *   <li>条件规则：由上行数据处理链路调用 {@link #evaluateCondition}，按测点读数评估触发与恢复；</li>
 *   <li>组合规则：手动触发，由 ControlRuleService 执行。</li>
 * </ul>
 * 条件规则的"当前是否处于触发态"按该规则最近一条 RULE 来源记录的动作推断：
 * 最后动作 = 规则动作 → 触发中；否则未触发。
 */
@Service
public class ControlRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(ControlRuleEngine.class);

    private final ControlRuleRepository ruleRepository;
    private final ControlLogRepository logRepository;
    private final ControlExecutionService executionService;

    public ControlRuleEngine(ControlRuleRepository ruleRepository,
                             ControlLogRepository logRepository,
                             ControlExecutionService executionService) {
        this.ruleRepository = ruleRepository;
        this.logRepository = logRepository;
        this.executionService = executionService;
    }

    /** 每分钟第 0 秒扫描定时规则。 */
    @Scheduled(cron = "0 * * * * *")
    public void scanTimedRules() {
        Date now = new Date();
        String hhmm = String.format("%02d:%02d",
                now.toInstant().atZone(ZoneId.systemDefault()).getHour(),
                now.toInstant().atZone(ZoneId.systemDefault()).getMinute());
        List<ControlRule> rules = ruleRepository.findAll(Wrappers.<ControlRule>lambdaQuery()
                .eq(ControlRule::getEnabled, true)
                .eq(ControlRule::getTrigger, ConflictChecker.TRIGGER_TIMED));
        for (ControlRule rule : rules) {
            if (hhmm.equals(rule.getTime())) {
                fireTimed(rule, rule.getAction(), hhmm);
            }
            if (rule.getCloseTime() != null && !rule.getCloseTime().isEmpty()
                    && hhmm.equals(rule.getCloseTime())) {
                fireTimed(rule, ConflictChecker.opposite(rule.getAction()), hhmm);
            }
        }
    }

    private void fireTimed(ControlRule rule, String action, String hhmm) {
        if (executedToday(rule.getId(), action)) {
            return;
        }
        log.info("timed rule fired: rule={} action={} time={}", rule.getName(), action, hhmm);
        for (GatewayEntry entry : executionService.parseDevices(rule.getDevices())) {
            executionService.executeDevice(entry, action, null, rule.getId(), rule.getName(),
                    ControlExecutionService.SOURCE_RULE, rule.getPlotId(), rule.getOrgCode());
        }
    }

    private boolean executedToday(String ruleId, String action) {
        Date start = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        long count = logRepository.count(Wrappers.<ControlLog>lambdaQuery()
                .eq(ControlLog::getRuleId, ruleId)
                .eq(ControlLog::getAction, action)
                .eq(ControlLog::getSource, ControlExecutionService.SOURCE_RULE)
                .ge(ControlLog::getCreatedAt, start));
        return count > 0;
    }

    /**
     * 条件规则评估：一条测点读数到来时，对该测点上全部启用的条件规则评估触发/恢复。
     *
     * @param metricDeviceId 取数传感器设备编号（station）
     * @param metric         指标键（temp/humid/press 等）
     * @param value          读数值
     * @return 触发/恢复下发的记录数
     */
    public int evaluateCondition(String metricDeviceId, String metric, double value) {
        if (metricDeviceId == null || metric == null || metric.isEmpty()) {
            return 0;
        }
        List<ControlRule> rules = ruleRepository.findAll(Wrappers.<ControlRule>lambdaQuery()
                .eq(ControlRule::getEnabled, true)
                .eq(ControlRule::getTrigger, ConflictChecker.TRIGGER_CONDITION)
                .eq(ControlRule::getMetricDeviceId, metricDeviceId)
                .eq(ControlRule::getMetric, metric));
        int fired = 0;
        for (ControlRule rule : rules) {
            if (!inWindow(rule)) {
                continue;
            }
            boolean triggered = isCurrentlyTriggered(rule);
            if (!triggered && predicateMatches(value, rule.getOperator(), rule.getThreshold())) {
                fireCondition(rule, rule.getAction(), String.valueOf(value));
                fired++;
            } else if (triggered && Boolean.TRUE.equals(rule.getUseReturn())) {
                String revOp = rule.getOperator() != null && rule.getOperator().startsWith(">") ? "<" : ">";
                if (predicateMatches(value, revOp, rule.getReturnThreshold())) {
                    fireCondition(rule, ConflictChecker.opposite(rule.getAction()), String.valueOf(value));
                    fired++;
                }
            }
        }
        return fired;
    }

    private boolean inWindow(ControlRule rule) {
        if (!inEffectiveDates(rule)) {
            return false;
        }
        int now = ConflictChecker.minute(String.format("%02d:%02d",
                new Date().toInstant().atZone(ZoneId.systemDefault()).getHour(),
                new Date().toInstant().atZone(ZoneId.systemDefault()).getMinute()));
        int start = ConflictChecker.minute(rule.getStartTime());
        int end = ConflictChecker.minute(rule.getEndTime());
        return start >= 0 && end >= 0 && now >= start && now <= end;
    }

    /** 条件规则的生效日期范围；未配置任何日期时视为始终有效。 */
    private boolean inEffectiveDates(ControlRule rule) {
        String from = rule.getEffectiveFrom();
        String to = rule.getEffectiveTo();
        if ((from == null || from.isEmpty()) && (to == null || to.isEmpty())) {
            return true;
        }
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (from != null && !from.isEmpty() && today.compareTo(from) < 0) {
            return false;
        }
        return to == null || to.isEmpty() || today.compareTo(to) <= 0;
    }

    private boolean isCurrentlyTriggered(ControlRule rule) {
        List<ControlLog> latest = logRepository.findAll(Wrappers.<ControlLog>lambdaQuery()
                .eq(ControlLog::getRuleId, rule.getId())
                .eq(ControlLog::getSource, ControlExecutionService.SOURCE_RULE)
                .orderByDesc(ControlLog::getCreatedAt)
                .last("LIMIT 1"));
        return !latest.isEmpty() && rule.getAction().equals(latest.get(0).getAction());
    }

    private void fireCondition(ControlRule rule, String action, String reading) {
        log.info("condition rule fired: rule={} action={} reading={}", rule.getName(), action, reading);
        for (GatewayEntry entry : executionService.parseDevices(rule.getDevices())) {
            executionService.executeDevice(entry, action, null, rule.getId(), rule.getName(),
                    ControlExecutionService.SOURCE_RULE, rule.getPlotId(), rule.getOrgCode());
        }
    }

    /** 谓词判定：&gt; / &gt;= / &lt; / &lt;=（与原型一致）。 */
    public static boolean predicateMatches(double value, String operator, BigDecimal threshold) {
        if (operator == null || threshold == null) {
            return false;
        }
        double t = threshold.doubleValue();
        switch (operator) {
            case ">": return value > t;
            case ">=": return value >= t;
            case "<": return value < t;
            case "<=": return value <= t;
            default: return false;
        }
    }
}
