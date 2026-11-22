package com.iot.platform.service.control;

import com.iot.platform.model.ControlRule;
import com.iot.platform.service.control.ConflictChecker.Candidate;
import com.iot.platform.service.control.ConflictChecker.ExistingRule;

import java.util.ArrayList;
import java.util.List;

/** 控制规则实体 ↔ 冲突校验视图的映射助手。 */
public final class ControlRuleAssembler {

    private ControlRuleAssembler() {
    }

    public static Candidate toCandidate(ControlRule rule, List<DeviceEntry> entries) {
        Candidate c = new Candidate();
        c.id = rule.getId();
        c.name = rule.getName();
        c.kind = rule.getKind();
        c.trigger = rule.getTrigger();
        for (DeviceEntry e : entries) {
            c.devices.add(e.getDeviceId());
            c.deviceTypes.add(e.getDeviceType());
        }
        c.action = rule.getAction();
        c.time = rule.getTime();
        c.closeTime = rule.getCloseTime();
        c.metric = rule.getMetric();
        c.metricDeviceId = rule.getMetricDeviceId();
        c.operator = rule.getOperator();
        c.threshold = rule.getThreshold();
        c.start = rule.getStartTime();
        c.end = rule.getEndTime();
        c.useReturn = Boolean.TRUE.equals(rule.getUseReturn());
        c.returnThreshold = rule.getReturnThreshold();
        return c;
    }

    public static ExistingRule toExisting(ControlRule rule, List<DeviceEntry> entries) {
        List<String> deviceIds = new ArrayList<>();
        for (DeviceEntry e : entries) {
            deviceIds.add(e.getDeviceId());
        }
        String sensor = rule.getMetric() == null || rule.getMetric().isEmpty() ? null
                : (rule.getMetricDeviceId() == null ? "" : rule.getMetricDeviceId()) + ":" + rule.getMetric();
        return new ExistingRule(rule.getId(), rule.getName(), rule.getTrigger(), deviceIds, rule.getAction(),
                rule.getTime(), rule.getCloseTime(), sensor, rule.getOperator(), rule.getThreshold(),
                rule.getStartTime(), rule.getEndTime(), Boolean.TRUE.equals(rule.getUseReturn()),
                rule.getReturnThreshold(), ControlRuleSummaries.summary(rule));
    }
}
