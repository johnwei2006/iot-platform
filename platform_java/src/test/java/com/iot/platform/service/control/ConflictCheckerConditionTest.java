package com.iot.platform.service.control;

import com.iot.platform.service.control.ConflictChecker.Candidate;
import com.iot.platform.service.control.ConflictChecker.ConflictResult;
import com.iot.platform.service.control.ConflictChecker.ExistingRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 冲突校验单测（条件规则/组合场景）。 */
class ConflictCheckerConditionTest {

    private Candidate conditionCandidate(String action, String sensorDevice, String operator,
                                         double threshold, boolean useReturn, double returnThreshold) {
        Candidate c = new Candidate();
        c.name = "高温规则";
        c.kind = ConflictChecker.KIND_CONDITION;
        c.trigger = ConflictChecker.TRIGGER_CONDITION;
        c.devices.addAll(List.of("wind1"));
        c.deviceTypes.add("风机");
        c.action = action;
        c.metric = "temp";
        c.metricDeviceId = sensorDevice;
        c.operator = operator;
        c.threshold = BigDecimal.valueOf(threshold);
        c.start = "00:00";
        c.end = "23:59";
        c.useReturn = useReturn;
        c.returnThreshold = BigDecimal.valueOf(returnThreshold);
        return c;
    }

    private ExistingRule condition(String action, String sensor, String operator, double threshold) {
        return new ExistingRule("r1", "风机温控", ConflictChecker.TRIGGER_CONDITION, List.of("wind1"),
                action, null, null, sensor, operator, BigDecimal.valueOf(threshold),
                "00:00", "23:59", false, null, "风机温控");
    }

    @Test
    void conditionOverlappingPredicateSameSensorIsSoft() {
        ExistingRule existing = condition("ON", "GW1:temp", ">=", 34);
        ConflictResult r = ConflictChecker.check(conditionCandidate("ON", "GW1", ">", 32, false, 0),
                "", List.of(existing));
        assertFalse(r.blocked());
        assertEquals(1, r.getHits().size());
        assertFalse(r.getHits().get(0).isHard());
    }

    @Test
    void conditionOppositeActionOnSameSensorIsHard() {
        ExistingRule existing = condition("ON", "GW1:temp", ">=", 34);
        assertTrue(ConflictChecker.check(conditionCandidate("OFF", "GW1", ">", 32, false, 0),
                "", List.of(existing)).blocked());
    }

    @Test
    void conditionDifferentSensorAlwaysOverlaps() {
        // 原型 predicatesOverlap：测点不同视为重叠（保守判定）
        ExistingRule existing = condition("ON", "GW2:temp", ">=", 34);
        assertTrue(ConflictChecker.check(conditionCandidate("OFF", "GW3", ">", 32, false, 0),
                "", List.of(existing)).blocked());
    }

    @Test
    void manualCombosSameMembersSameActionIsDuplicate() {
        ExistingRule group = new ExistingRule("g1", "早间通风组", ConflictChecker.TRIGGER_MANUAL,
                List.of("wind1", "wind2"), "ON", null, null, null, null, null, null, null,
                false, null, "手动整组开启");
        Candidate c = new Candidate();
        c.name = "重复组合";
        c.kind = ConflictChecker.KIND_GROUP;
        c.trigger = ConflictChecker.TRIGGER_MANUAL;
        c.devices.addAll(List.of("wind2", "wind1"));
        c.deviceTypes.add("风机");
        c.action = "ON";
        ConflictResult r = ConflictChecker.check(c, "", List.of(group));
        assertFalse(r.blocked());
        assertEquals(1, r.getHits().size());
    }
}
