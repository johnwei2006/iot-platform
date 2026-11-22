package com.iot.platform.service.control;

import com.iot.platform.model.ControlLog;
import com.iot.platform.model.ControlRule;
import com.iot.platform.repository.ControlLogRepository;
import com.iot.platform.repository.ControlRuleRepository;
import com.iot.platform.service.DownlinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 条件规则评估单测：触发、恢复与窗口内评估（复刻原型场景）。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ControlRuleEngineConditionTest {

    @Mock
    private ControlRuleRepository ruleRepository;

    @Mock
    private ControlLogRepository logRepository;

    @Mock
    private DownlinkService downlinkService;

    private ControlRuleEngine engine;

    private ControlRule rule;

    @BeforeEach
    void setUp() {
        ControlExecutionService executionService = new ControlExecutionService(downlinkService,
                logRepository, new ObjectMapper());
        engine = new ControlRuleEngine(ruleRepository, logRepository, executionService);

        rule = new ControlRule();
        rule.setId("r1");
        rule.setName("fan temp control");
        rule.setKind(ConflictChecker.KIND_CONDITION);
        rule.setTrigger(ConflictChecker.TRIGGER_CONDITION);
        rule.setAction("ON");
        rule.setMetric("temp");
        rule.setMetricDeviceId("GW1");
        rule.setOperator(">");
        rule.setThreshold(BigDecimal.valueOf(34));
        rule.setStartTime("00:00");
        rule.setEndTime("23:59");
        rule.setUseReturn(true);
        rule.setReturnThreshold(BigDecimal.valueOf(30));
        rule.setEnabled(true);
        rule.setDevices("[{\"deviceId\":\"GW1\",\"nodeId\":\"NODE1\",\"channel\":1," +
                "\"deviceName\":\"fan 1\",\"deviceType\":\"FAN\"}]");

        when(ruleRepository.findAll(any())).thenReturn(List.of(rule));
        when(logRepository.findAll(any())).thenReturn(List.of());
        when(logRepository.count(any())).thenReturn(0L);
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void firesTriggerWhenReadingExceedsThreshold() {
        int fired = engine.evaluateCondition("GW1", "temp", 35.6);
        assertEquals(1, fired);
        verify(downlinkService).send(any());
        verify(logRepository).save(any());
    }

    @Test
    void noFireWhenReadingWithinThreshold() {
        int fired = engine.evaluateCondition("GW1", "temp", 29.7);
        assertEquals(0, fired);
        verify(downlinkService, never()).send(any());
    }

    @Test
    void firesRecoveryWhenTriggeredAndReadingDropsBelowReturnThreshold() {
        ControlLog latest = new ControlLog();
        latest.setAction("ON");
        latest.setSource(ControlExecutionService.SOURCE_RULE);
        when(logRepository.findAll(any())).thenReturn(List.of(latest));

        int fired = engine.evaluateCondition("GW1", "temp", 29.7);
        assertEquals(1, fired);
        verify(downlinkService).send(any());
    }

    @Test
    void noFireWhenRuleNotEnabledOrMetricMismatch() {
        // 仓库查询按 enabled / metricDeviceId / metric 过滤，Mock 直接返回空集模拟"无匹配规则"
        when(ruleRepository.findAll(any())).thenReturn(List.of());
        assertEquals(0, engine.evaluateCondition("GW1", "temp", 35.6));
        assertEquals(0, engine.evaluateCondition("GW2", "temp", 35.6));
        assertEquals(0, engine.evaluateCondition("GW1", "humid", 35.6));
        verify(downlinkService, never()).send(any());
    }

    @Test
    void predicateMatchesCoversOperators() {
        assertTrue(ControlRuleEngine.predicateMatches(34.1, ">", BigDecimal.valueOf(34)));
        assertTrue(ControlRuleEngine.predicateMatches(34, ">=", BigDecimal.valueOf(34)));
        assertTrue(ControlRuleEngine.predicateMatches(33.9, "<", BigDecimal.valueOf(34)));
        assertTrue(ControlRuleEngine.predicateMatches(34, "<=", BigDecimal.valueOf(34)));
        assertEquals(false, ControlRuleEngine.predicateMatches(34, ">", BigDecimal.valueOf(34)));
    }
}