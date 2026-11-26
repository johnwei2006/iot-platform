package com.iot.platform.service.control;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.ControlLog;
import com.iot.platform.model.ControlRule;
import com.iot.platform.repository.ControlLogRepository;
import com.iot.platform.repository.ControlRuleRepository;
import com.iot.platform.service.control.ConflictChecker.Candidate;
import com.iot.platform.service.control.ConflictChecker.ConflictResult;
import com.iot.platform.service.control.ConflictChecker.ExistingRule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 控制规则应用服务：结构校验 + 服务端冲突校验（唯一权威）+ 保存/启停/整组执行。
 * intent 三态：CHECK（只校验）/ DRAFT（保存停用草稿）/ ENABLE（保存并启用，硬冲突阻止）。
 */
@Service
public class ControlRuleService {

    public static final String INTENT_CHECK = "CHECK";
    public static final String INTENT_DRAFT = "DRAFT";
    public static final String INTENT_ENABLE = "ENABLE";

    private final ControlRuleRepository ruleRepository;
    private final ControlLogRepository logRepository;
    private final ControlExecutionService executionService;
    private final ObjectMapper objectMapper;

    public ControlRuleService(ControlRuleRepository ruleRepository,
                              ControlLogRepository logRepository,
                              ControlExecutionService executionService,
                              ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.logRepository = logRepository;
        this.executionService = executionService;
        this.objectMapper = objectMapper;
    }

    /** 保存或更新规则；返回冲突结果与规则（CHECK 或被硬冲突阻止时不落库）。 */
    public SaveOutcome save(ControlRule rule, String intent) {
        normalize(rule);
        Candidate candidate = ControlRuleAssembler.toCandidate(rule, parseDevicesJson(rule.getDevices()));
        String error = ConflictChecker.validate(candidate, takenNames(rule.getId()));
        ConflictResult result = ConflictChecker.check(candidate, error, enabledExistingRules());
        if (INTENT_CHECK.equals(intent) || result.blocked()) {
            return new SaveOutcome(result, rule, false);
        }
        rule.setEnabled(INTENT_ENABLE.equals(intent));
        Date now = new Date();
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(now);
        }
        rule.setUpdatedAt(now);
        if (rule.getId() == null || rule.getId().isEmpty()) {
            rule.setId(UUID.randomUUID().toString());
        }
        return new SaveOutcome(result, ruleRepository.save(rule), true);
    }

    /** 启停规则：启用时硬冲突/校验失败抛 IllegalStateException（Controller 转 400）。 */
    public ControlRule toggle(String id) {
        ControlRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在"));
        if (Boolean.TRUE.equals(rule.getEnabled())) {
            rule.setEnabled(false);
            rule.setUpdatedAt(new Date());
            return ruleRepository.save(rule);
        }
        Candidate candidate = ControlRuleAssembler.toCandidate(rule, parseDevicesJson(rule.getDevices()));
        String error = ConflictChecker.validate(candidate, takenNames(rule.getId()));
        ConflictResult result = ConflictChecker.check(candidate, error, enabledExistingRules());
        if (result.blocked()) {
            throw new IllegalStateException("存在控制冲突，无法启用");
        }
        rule.setEnabled(true);
        rule.setUpdatedAt(new Date());
        return ruleRepository.save(rule);
    }

    /** 执行规则：组合=整组下发；单设备规则按规则动作下发。 */
    public List<ControlLog> execute(ControlRule rule, String source) {
        if (!Boolean.TRUE.equals(rule.getEnabled())) {
            throw new IllegalStateException("请先校验并启用规则");
        }
        String action = rule.getAction() == null ? "ON" : rule.getAction();
        List<ControlLog> logs = new ArrayList<>();
        for (GatewayEntry entry : parseDevicesJson(rule.getDevices())) {
            logs.add(executionService.executeDevice(entry, action, null, rule.getId(), rule.getName(),
                    source, rule.getPlotId(), rule.getOrgCode()));
        }
        return logs;
    }

    public ControlRule get(String id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在"));
    }

    public void delete(String id) {
        ruleRepository.deleteById(id);
    }

    public List<GatewayEntry> parseDevicesJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<GatewayEntry>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    private void normalize(ControlRule rule) {
        rule.setName(rule.getName() == null ? null : rule.getName().trim());
        if (rule.getKind() == null) {
            rule.setKind(rule.getTrigger());
        }
        if (rule.getTrigger() == null) {
            rule.setTrigger(rule.getKind());
        }
        if (rule.getUseReturn() == null) {
            rule.setUseReturn(false);
        }
    }

    private Set<String> takenNames(String excludeId) {
        Set<String> names = new HashSet<>();
        for (ControlRule r : ruleRepository.findAll()) {
            if (r.getId() != null && r.getId().equals(excludeId)) continue;
            if (r.getName() != null) names.add(r.getName().trim());
        }
        return names;
    }

    private List<ExistingRule> enabledExistingRules() {
        List<ExistingRule> list = new ArrayList<>();
        for (ControlRule r : ruleRepository.findAll(Wrappers.<ControlRule>lambdaQuery()
                .eq(ControlRule::getEnabled, true))) {
            list.add(ControlRuleAssembler.toExisting(r, parseDevicesJson(r.getDevices())));
        }
        return list;
    }

    /** 保存结果：冲突判定 + 规则 + 是否已落库。 */
    public static class SaveOutcome {
        public final ConflictResult result;
        public final ControlRule rule;
        public final boolean saved;

        public SaveOutcome(ConflictResult result, ControlRule rule, boolean saved) {
            this.result = result;
            this.rule = rule;
            this.saved = saved;
        }
    }
}
