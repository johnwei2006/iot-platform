package com.iot.platform.service.control;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 控制冲突校验：服务端唯一权威，复刻原型 ruleEvents / predicatesOverlap / conflictCheck 算法。
 *
 * <p>判定规则（与原型一致）：
 * <ul>
 *   <li>定时 → 当日时间点事件（执行动作 + 可选反向动作时间）；</li>
 *   <li>条件 → 生效时段 [start, end] 内触发事件与可选恢复事件；</li>
 *   <li>共享设备且事件区间有交集时：动作相反 = 硬冲突（阻止启用）；动作相同 = 同向重复（仅提示）；</li>
 *   <li>组合：成员集合相同且动作相同的两条手动规则 = 同向重复。</li>
 * </ul>
 * 本类不依赖 Spring，便于单测。
 */
public final class ConflictChecker {

    public static final String KIND_GROUP = "GROUP";
    public static final String KIND_TIMED = "TIMED";
    public static final String KIND_CONDITION = "CONDITION";

    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_TIMED = "TIMED";
    public static final String TRIGGER_CONDITION = "CONDITION";

    private static final Pattern TIME = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private ConflictChecker() {
    }

    /** 校验/冲突判定用的候选视图（实体或表单均可映射成此结构）。 */
    public static class Candidate {
        public String id;
        public String name;
        public String kind;
        public String trigger;
        public List<String> devices = new ArrayList<>();
        public List<String> deviceTypes = new ArrayList<>();
        public String action;
        public String time;
        public String closeTime;
        public String metric;
        public String metricDeviceId;
        public String operator;
        public BigDecimal threshold;
        public String start;
        public String end;
        public boolean useReturn;
        public BigDecimal returnThreshold;

        /** 条件规则的取数测点标识 metricDeviceId:metric；非条件规则返回 null（时间事件）。 */
        public String sensorKey() {
            if (metric == null || metric.isEmpty()) {
                return null;
            }
            String device = metricDeviceId == null ? "" : metricDeviceId;
            return device + ":" + metric;
        }
    }

    /** 规则事件：[start, end] 分钟区间 + 动作 + 条件谓词（sensor 为空表示时间事件）。 */
    public static class RuleEvent {
        final int start;
        final int end;
        final String action;
        final String sensor;
        final String operator;
        final BigDecimal threshold;

        RuleEvent(int start, int end, String action) {
            this(start, end, action, null, null, null);
        }

        RuleEvent(int start, int end, String action, String sensor, String operator, BigDecimal threshold) {
            this.start = start;
            this.end = end;
            this.action = action;
            this.sensor = sensor;
            this.operator = operator;
            this.threshold = threshold;
        }
    }

    public static class ConflictHit {
        private final String ruleId;
        private final String ruleName;
        private final boolean hard;
        private final String detail;

        public ConflictHit(String ruleId, String ruleName, boolean hard, String detail) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.hard = hard;
            this.detail = detail;
        }

        public String getRuleId() { return ruleId; }
        public String getRuleName() { return ruleName; }
        public boolean isHard() { return hard; }
        public String getDetail() { return detail; }
    }

    public static class ConflictResult {
        private final String error;
        private final List<ConflictHit> hits;

        public ConflictResult(String error, List<ConflictHit> hits) {
            this.error = error;
            this.hits = hits;
        }

        public String getError() { return error; }
        public List<ConflictHit> getHits() { return hits; }

        public boolean blocked() {
            return (error != null && !error.isEmpty()) || hits.stream().anyMatch(ConflictHit::isHard);
        }
    }

    /** 已有启用规则摘要，用于与候选做冲突比对。 */
    public static class ExistingRule {
        public final String id;
        public final String name;
        public final String trigger;
        public final List<String> devices;
        public final String action;
        public final String time;
        public final String closeTime;
        public final String sensor;
        public final String operator;
        public final BigDecimal threshold;
        public final String start;
        public final String end;
        public final boolean useReturn;
        public final BigDecimal returnThreshold;
        public final String summary;

        public ExistingRule(String id, String name, String trigger, List<String> devices, String action,
                            String time, String closeTime, String sensor, String operator, BigDecimal threshold,
                            String start, String end, boolean useReturn, BigDecimal returnThreshold, String summary) {
            this.id = id;
            this.name = name;
            this.trigger = trigger;
            this.devices = devices;
            this.action = action;
            this.time = time;
            this.closeTime = closeTime;
            this.sensor = sensor;
            this.operator = operator;
            this.threshold = threshold;
            this.start = start;
            this.end = end;
            this.useReturn = useReturn;
            this.returnThreshold = returnThreshold;
            this.summary = summary;
        }
    }

    /**
     * 结构校验（复刻原型 validateControl）。
     *
     * @param takenNames 除候选自身外的全部规则名称（含停用规则）
     * @return 错误描述；空串表示通过
     */
    public static String validate(Candidate rule, Set<String> takenNames) {
        if (rule.name == null || rule.name.trim().isEmpty()) return "请填写名称";
        if (KIND_GROUP.equals(rule.kind) && !TRIGGER_MANUAL.equals(rule.trigger)) return "组合控制仅支持手动触发";
        if (!KIND_GROUP.equals(rule.kind) && rule.devices.size() != 1) return "自动控制请选择单台设备";
        if (rule.devices.isEmpty() || rule.devices.contains(null) || rule.devices.contains("")) return "请选择已授权的设备";
        Set<String> types = new HashSet<>(rule.deviceTypes);
        types.remove(null);
        types.remove("");
        if (types.size() != 1) return "组合只支持同类型设备，请分开设置";
        if (KIND_GROUP.equals(rule.kind) && rule.devices.size() < 2) return "组合至少选择两台同类型设备";
        if (!"ON".equals(rule.action) && !"OFF".equals(rule.action)) return "请选择执行动作";
        String trimmed = rule.name.trim();
        for (String taken : takenNames) {
            if (taken != null && taken.equals(trimmed)) return "该名称已存在，请更换名称";
        }
        if (TRIGGER_TIMED.equals(rule.trigger)) {
            if (!validMinute(rule.time)) return "请选择有效执行时间";
            if (rule.closeTime != null && !rule.closeTime.isEmpty()
                    && (!validMinute(rule.closeTime) || rule.closeTime.equals(rule.time))) {
                return "反向动作时间应与执行时间不同";
            }
        }
        if (TRIGGER_CONDITION.equals(rule.trigger)) {
            if (rule.metric == null || rule.metric.isEmpty()
                    || !List.of(">", ">=", "<", "<=").contains(rule.operator)
                    || rule.threshold == null) {
                return "请选择有效测点并填写数值阈值";
            }
            if (!validMinute(rule.start) || !validMinute(rule.end)
                    || minute(rule.start) >= minute(rule.end)) {
                return "允许触发开始必须早于结束，请调整时段";
            }
            if (rule.useReturn) {
                boolean high = rule.operator.startsWith(">");
                if (rule.returnThreshold == null
                        || (high ? rule.returnThreshold.compareTo(rule.threshold) >= 0
                                 : rule.returnThreshold.compareTo(rule.threshold) <= 0)) {
                    return high ? "恢复阈值必须低于触发阈值" : "恢复阈值必须高于触发阈值";
                }
            }
        }
        return "";
    }

    /** 复刻原型 ruleEvents：规则在一天内产生的事件集合。 */
    public static List<RuleEvent> ruleEvents(String trigger, String action, String time, String closeTime,
                                             String sensor, String operator, BigDecimal threshold,
                                             String start, String end, boolean useReturn, BigDecimal returnThreshold) {
        List<RuleEvent> events = new ArrayList<>();
        if (TRIGGER_MANUAL.equals(trigger)) {
            return events;
        }
        if (TRIGGER_TIMED.equals(trigger)) {
            int t = minute(time);
            if (t >= 0) {
                events.add(new RuleEvent(t, t, action));
            }
            if (closeTime != null && !closeTime.isEmpty() && validMinute(closeTime)) {
                int ct = minute(closeTime);
                events.add(new RuleEvent(ct, ct, opposite(action)));
            }
            return events;
        }
        int s = minute(start);
        int e = minute(end);
        if (s >= 0 && e >= 0) {
            events.add(new RuleEvent(s, e, action, sensor, operator, threshold));
            if (useReturn) {
                String revOp = operator != null && operator.startsWith(">") ? "<" : ">";
                events.add(new RuleEvent(s, e, opposite(action), sensor, revOp, returnThreshold));
            }
        }
        return events;
    }

    /** 复刻原型 predicatesOverlap：谓词测点不同视为重叠；同测点按数值区间求交。 */
    public static boolean predicatesOverlap(RuleEvent a, RuleEvent b) {
        if (a.sensor == null || b.sensor == null || !a.sensor.equals(b.sensor)) return true;
        double[] x = interval(a);
        double[] y = interval(b);
        return x[0] < y[1] && y[0] < x[1];
    }

    private static double[] interval(RuleEvent e) {
        if (e.operator != null && e.operator.startsWith(">")) {
            return new double[]{e.threshold.doubleValue(), Double.POSITIVE_INFINITY};
        }
        return new double[]{Double.NEGATIVE_INFINITY, e.threshold.doubleValue()};
    }

    /**
     * 复刻原型 conflictCheck：候选与既有启用规则的冲突判定。
     *
     * @param error 结构校验错误（由 {@link #validate} 产出）；非空直接短路返回
     */
    public static ConflictResult check(Candidate candidate, String error, List<ExistingRule> enabledRules) {
        if (error != null && !error.isEmpty()) {
            return new ConflictResult(error, new ArrayList<>());
        }
        List<RuleEvent> candidateEvents = ruleEvents(candidate.trigger, candidate.action, candidate.time,
                candidate.closeTime, candidate.sensorKey(), candidate.operator, candidate.threshold,
                candidate.start, candidate.end, candidate.useReturn, candidate.returnThreshold);
        List<ConflictHit> hits = new ArrayList<>();
        for (ExistingRule rule : enabledRules) {
            if (rule.id != null && rule.id.equals(candidate.id)) continue;
            List<String> shared = new ArrayList<>(candidate.devices);
            shared.retainAll(rule.devices);
            if (shared.isEmpty()) continue;
            boolean hard = false;
            boolean overlap = false;
            for (RuleEvent a : candidateEvents) {
                for (RuleEvent b : ruleEvents(rule.trigger, rule.action, rule.time, rule.closeTime,
                        rule.sensor, rule.operator, rule.threshold, rule.start, rule.end,
                        rule.useReturn, rule.returnThreshold)) {
                    if (a.start <= b.end && b.start <= a.end && predicatesOverlap(a, b)) {
                        overlap = true;
                        if (!a.action.equals(b.action)) hard = true;
                    }
                }
            }
            if (TRIGGER_MANUAL.equals(candidate.trigger) && TRIGGER_MANUAL.equals(rule.trigger)
                    && sortedEquals(candidate.devices, rule.devices)
                    && candidate.action.equals(rule.action)) {
                overlap = true;
            }
            if (overlap) {
                String detail = rule.summary + " · "
                        + (hard ? "触发范围有交集，执行相反动作" : "触发范围或组合成员重复，执行相同动作");
                hits.add(new ConflictHit(rule.id, rule.name, hard, detail));
            }
        }
        hits.sort((x, y) -> Boolean.compare(y.isHard(), x.isHard()));
        return new ConflictResult("", hits);
    }

    public static String opposite(String action) {
        return "ON".equals(action) ? "OFF" : "ON";
    }

    public static boolean validMinute(String value) {
        return value != null && TIME.matcher(value).matches();
    }

    public static int minute(String value) {
        if (!validMinute(value)) return -1;
        String[] parts = value.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private static boolean sortedEquals(List<String> a, List<String> b) {
        List<String> x = new ArrayList<>(a);
        List<String> y = new ArrayList<>(b);
        x.sort(String::compareTo);
        y.sort(String::compareTo);
        return x.equals(y);
    }
}
