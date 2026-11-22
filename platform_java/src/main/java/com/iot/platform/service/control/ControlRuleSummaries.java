package com.iot.platform.service.control;

import com.iot.platform.model.ControlRule;

/** 规则摘要文案（对齐原型 controlSummary，动作文案用开启/关闭）。 */
public final class ControlRuleSummaries {

    private ControlRuleSummaries() {
    }

    public static String summary(ControlRule rule) {
        String action = actionText(rule.getAction());
        if (ConflictChecker.TRIGGER_MANUAL.equals(rule.getTrigger())) {
            return "手动整组" + action;
        }
        if (ConflictChecker.TRIGGER_TIMED.equals(rule.getTrigger())) {
            String base = "每天 " + rule.getTime() + " " + action;
            if (rule.getCloseTime() != null && !rule.getCloseTime().isEmpty()) {
                base += "，" + rule.getCloseTime() + " " + actionText(ConflictChecker.opposite(rule.getAction()));
            }
            return base;
        }
        String base = rule.getStartTime() + "–" + rule.getEndTime() + "，"
                + metricText(rule.getMetric()) + " " + rule.getOperator() + " "
                + rule.getThreshold() + " 时" + action;
        if (Boolean.TRUE.equals(rule.getUseReturn())) {
            base += "，恢复至 " + rule.getReturnThreshold() + " 时"
                    + actionText(ConflictChecker.opposite(rule.getAction()));
        }
        return base;
    }

    public static String actionText(String action) {
        if ("ON".equals(action) || "OPEN".equals(action)) return "开启";
        return "关闭";
    }

    public static String metricText(String metric) {
        if (metric == null) return "";
        switch (metric) {
            case "temp": return "空气温度";
            case "humid": return "空气湿度";
            case "soil": return "土壤温度";
            case "water": return "土壤湿度";
            case "press": return "气压";
            default: return metric;
        }
    }
}
