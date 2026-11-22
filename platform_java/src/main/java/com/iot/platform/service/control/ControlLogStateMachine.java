package com.iot.platform.service.control;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.ControlLog;
import com.iot.platform.repository.ControlLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * ControlLog 状态机终态迁移：
 * <ul>
 *   <li>TIMEOUT：定时扫描 PENDING 超时（默认 120s，app.control.timeout-seconds）；</li>
 *   <li>SUCCESS / FAILED：由 0x1A 反馈处理调用 {@link #applyFeedback}；</li>
 *   <li>PARTIAL：组合执行按组内各设备记录聚合（{@link #aggregateResult}）。</li>
 * </ul>
 */
@Component
public class ControlLogStateMachine {

    private final ControlLogRepository controlLogRepository;
    private final ControlExecutionService executionService;

    public ControlLogStateMachine(ControlLogRepository controlLogRepository,
                                  ControlExecutionService executionService) {
        this.controlLogRepository = controlLogRepository;
        this.executionService = executionService;
    }

    /** 每分钟扫描一次 PENDING 超时记录，标记 TIMEOUT。 */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void scanTimeout() {
        Date deadline = new Date(System.currentTimeMillis() - executionService.getTimeoutSeconds() * 1000);
        List<ControlLog> pending = controlLogRepository.findAll(Wrappers.<ControlLog>lambdaQuery()
                .eq(ControlLog::getResult, ControlExecutionService.RESULT_PENDING)
                .lt(ControlLog::getCreatedAt, deadline));
        for (ControlLog clog : pending) {
            clog.setResult(ControlExecutionService.RESULT_TIMEOUT);
            clog.setDetail("设备暂未回报，反馈超时。请刷新设备状态");
            clog.setFinishedAt(new Date());
            controlLogRepository.save(clog);
        }
    }

    /** 应用 0x1A 设备反馈：匹配的 PENDING 记录按 channelResult 迁移终态。 */
    public boolean applyFeedback(ControlLog clog, int channelResult) {
        if (!ControlExecutionService.RESULT_PENDING.equals(clog.getResult())) {
            return false;
        }
        if (channelResult == ControlExecutionService.FEEDBACK_OK) {
            clog.setResult(ControlExecutionService.RESULT_SUCCESS);
            clog.setDetail("设备已回报执行成功");
        } else {
            clog.setResult(ControlExecutionService.RESULT_FAILED);
            clog.setDetail("设备回报失败（code=" + channelResult + "），保留最后一次上报状态");
        }
        clog.setFinishedAt(new Date());
        controlLogRepository.save(clog);
        return true;
    }

    /** 组合执行聚合：全部成功=SUCCESS；全部失败=FAILED；部分成功=PARTIAL；未全部终态=PENDING。 */
    public static String aggregateResult(List<ControlLog> groupLogs) {
        if (groupLogs == null || groupLogs.isEmpty()) {
            return ControlExecutionService.RESULT_PENDING;
        }
        int success = 0;
        int failed = 0;
        for (ControlLog clog : groupLogs) {
            if (ControlExecutionService.RESULT_SUCCESS.equals(clog.getResult())) success++;
            else if (ControlExecutionService.RESULT_FAILED.equals(clog.getResult())) failed++;
        }
        int total = groupLogs.size();
        if (success == total) return ControlExecutionService.RESULT_SUCCESS;
        if (failed == total) return ControlExecutionService.RESULT_FAILED;
        if (success + failed == total) return ControlExecutionService.RESULT_PARTIAL;
        return ControlExecutionService.RESULT_PENDING;
    }
}
