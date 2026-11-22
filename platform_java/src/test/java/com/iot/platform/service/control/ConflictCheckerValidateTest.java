package com.iot.platform.service.control;

import com.iot.platform.service.control.ConflictChecker.Candidate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 结构校验单测（复刻原型 validateControl）。 */
class ConflictCheckerValidateTest {

    private Candidate base(String name, String kind, String trigger) {
        Candidate c = new Candidate();
        c.name = name;
        c.kind = kind;
        c.trigger = trigger;
        c.devices.addAll(List.of("wind1"));
        c.deviceTypes.add("风机");
        c.action = "ON";
        return c;
    }

    @Test
    void blankNameRejected() {
        Candidate c = base(" ", ConflictChecker.KIND_TIMED, ConflictChecker.TRIGGER_TIMED);
        c.time = "07:00";
        assertFalse(ConflictChecker.validate(c, Set.of()).isEmpty());
    }

    @Test
    void comboRequiresTwoSameTypeDevices() {
        Candidate single = base("单设备组合", ConflictChecker.KIND_GROUP, ConflictChecker.TRIGGER_MANUAL);
        assertEquals("组合至少选择两台同类型设备", ConflictChecker.validate(single, Set.of()));

        Candidate mixed = base("混类型", ConflictChecker.KIND_GROUP, ConflictChecker.TRIGGER_MANUAL);
        mixed.devices.add("blanket1");
        mixed.deviceTypes.add("棉被");
        assertTrue(ConflictChecker.validate(mixed, Set.of()).contains("同类型"));
    }

    @Test
    void autoRuleRequiresSingleDevice() {
        Candidate two = base("双设备定时", ConflictChecker.KIND_TIMED, ConflictChecker.TRIGGER_TIMED);
        two.devices.add("wind2");
        two.time = "07:00";
        assertEquals("自动控制请选择单台设备", ConflictChecker.validate(two, Set.of()));
    }

    @Test
    void duplicateNameRejected() {
        Candidate c = base("风机早间通风", ConflictChecker.KIND_TIMED, ConflictChecker.TRIGGER_TIMED);
        c.time = "07:00";
        assertEquals("该名称已存在，请更换名称", ConflictChecker.validate(c, Set.of("风机早间通风")));
    }

    @Test
    void timedRequiresValidDistinctTimes() {
        Candidate badTime = base("无效时间", ConflictChecker.KIND_TIMED, ConflictChecker.TRIGGER_TIMED);
        badTime.time = "25:00";
        assertEquals("请选择有效执行时间", ConflictChecker.validate(badTime, Set.of()));

        Candidate sameClose = base("同刻反向", ConflictChecker.KIND_TIMED, ConflictChecker.TRIGGER_TIMED);
        sameClose.time = "07:00";
        sameClose.closeTime = "07:00";
        assertEquals("反向动作时间应与执行时间不同", ConflictChecker.validate(sameClose, Set.of()));
    }

    @Test
    void conditionRequiresStartBeforeEnd() {
        Candidate c = base("时段错误", ConflictChecker.KIND_CONDITION, ConflictChecker.TRIGGER_CONDITION);
        c.metric = "temp";
        c.metricDeviceId = "GW1";
        c.operator = ">";
        c.threshold = BigDecimal.valueOf(32);
        c.start = "23:00";
        c.end = "07:00";
        assertEquals("允许触发开始必须早于结束，请调整时段", ConflictChecker.validate(c, Set.of()));
    }

    @Test
    void returnThresholdMustBeBeyondTrigger() {
        Candidate c = base("恢复阈值错误", ConflictChecker.KIND_CONDITION, ConflictChecker.TRIGGER_CONDITION);
        c.metric = "temp";
        c.metricDeviceId = "GW1";
        c.operator = ">";
        c.threshold = BigDecimal.valueOf(32);
        c.start = "00:00";
        c.end = "23:59";
        c.useReturn = true;
        c.returnThreshold = BigDecimal.valueOf(35);
        assertEquals("恢复阈值必须低于触发阈值", ConflictChecker.validate(c, Set.of()));
    }

    @Test
    void validRulePasses() {
        Candidate c = base("正确规则", ConflictChecker.KIND_TIMED, ConflictChecker.TRIGGER_TIMED);
        c.time = "07:00";
        c.closeTime = "07:20";
        assertEquals("", ConflictChecker.validate(c, Set.of()));
    }
}
