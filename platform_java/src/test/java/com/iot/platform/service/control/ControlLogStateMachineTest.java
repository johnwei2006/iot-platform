package com.iot.platform.service.control;

import com.iot.platform.model.ControlLog;
import com.iot.platform.repository.ControlLogRepository;
import com.iot.platform.service.DownlinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 控制记录状态机与下发执行单测（Mockito，无 Spring 上下文）。 */
@ExtendWith(MockitoExtension.class)
class ControlLogStateMachineTest {

    @Mock
    private ControlLogRepository logRepository;

    @Mock
    private DownlinkService downlinkService;

    private ControlExecutionService executionService() {
        return new ControlExecutionService(downlinkService, logRepository,
                new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private static ControlLog logOf(String id, String result) {
        ControlLog l = new ControlLog();
        l.setId(id);
        l.setResult(result);
        return l;
    }

    @Test
    void applyFeedbackTransitionsPendingToSuccessOrFailed() {
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ControlLogStateMachine sm = new ControlLogStateMachine(logRepository, executionService());
        ControlLog pending = logOf("l1", ControlExecutionService.RESULT_PENDING);

        assertTrue(sm.applyFeedback(pending, 0));
        assertEquals(ControlExecutionService.RESULT_SUCCESS, pending.getResult());
        assertFalse(sm.applyFeedback(pending, 0), "final state must not transition again");

        ControlLog failing = logOf("l2", ControlExecutionService.RESULT_PENDING);
        assertTrue(sm.applyFeedback(failing, 2));
        assertEquals(ControlExecutionService.RESULT_FAILED, failing.getResult());
        verify(logRepository, times(2)).save(any());
    }

    @Test
    void aggregateResultCoversAllGroupStates() {
        List<ControlLog> allSuccess = List.of(logOf("1", "SUCCESS"), logOf("2", "SUCCESS"));
        assertEquals(ControlExecutionService.RESULT_SUCCESS, ControlLogStateMachine.aggregateResult(allSuccess));

        List<ControlLog> allFailed = List.of(logOf("1", "FAILED"), logOf("2", "FAILED"));
        assertEquals(ControlExecutionService.RESULT_FAILED, ControlLogStateMachine.aggregateResult(allFailed));

        List<ControlLog> mixed = List.of(logOf("1", "SUCCESS"), logOf("2", "FAILED"));
        assertEquals(ControlExecutionService.RESULT_PARTIAL, ControlLogStateMachine.aggregateResult(mixed));

        List<ControlLog> unsettled = List.of(logOf("1", "SUCCESS"), logOf("2", "PENDING"));
        assertEquals(ControlExecutionService.RESULT_PENDING, ControlLogStateMachine.aggregateResult(unsettled));
    }

    @Test
    void manualDownlinkCreatesPendingLogAndFailureCreatesFailedLog() {
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ControlExecutionService svc = executionService();
        GatewayEntry entry = new GatewayEntry("GW1", "NODE1", 1, "fan 1", "FAN");

        ControlLog ok = svc.executeDevice(entry, "ON", null, null, null,
                ControlExecutionService.SOURCE_MANUAL, null, "ORG");
        assertEquals(ControlExecutionService.RESULT_PENDING, ok.getResult());
        assertEquals("GW1", ok.getDeviceId());

        org.mockito.Mockito.doThrow(new RuntimeException("MQ down")).when(downlinkService).send(any());
        ControlLog bad = svc.executeDevice(entry, "OFF", null, null, null,
                ControlExecutionService.SOURCE_MANUAL, null, "ORG");
        assertEquals(ControlExecutionService.RESULT_FAILED, bad.getResult());
    }
}