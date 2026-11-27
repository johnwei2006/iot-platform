package com.iot.platform.service.control;

import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.model.SensorMetadata;
import com.iot.platform.service.converter.SensorMetadataRegistry;
import com.iot.platform.service.uplink.UplinkHandler;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 0x18 传感器实时数据 → 条件规则评估。位于上行管线持久化之后（@Order(5)），
 * 旁路步骤：评估失败只告警，不影响上行 ACK。
 * channel 经 SensorMetadataRegistry 关联 sensorType，映射指标键：1→temp、2→humid、3→press。
 */
@Component
@Order(5)
public class ConditionRuleEvaluateHandler implements UplinkHandler {

    private static final Logger log = LoggerFactory.getLogger(ConditionRuleEvaluateHandler.class);

    /** dtm 协议：24 (0x18) 传感器实时数据 */
    public static final int SERVICE_TYPE_SENSOR_DATA = 24;

    private final SensorMetadataRegistry registry;
    private final ControlRuleEngine ruleEngine;

    public ConditionRuleEvaluateHandler(SensorMetadataRegistry registry, ControlRuleEngine ruleEngine) {
        this.registry = registry;
        this.ruleEngine = ruleEngine;
    }

    @Override
    public String name() {
        return "condition-rule-evaluate";
    }

    @Override
    public boolean critical() {
        return false;
    }

    @Override
    public void handle(UplinkMessage message) {
        if (message.getServiceType() != SERVICE_TYPE_SENSOR_DATA
                || message.getServiceDataDecoded() == null) {
            return;
        }
        JsonNode decoded = message.getServiceDataDecoded();
        JsonNode sensors = decoded.get("sensors");
        if (!decoded.isObject() || sensors == null || !sensors.isArray()) {
            return;
        }
        for (JsonNode sensor : sensors) {
            if (sensor == null || !sensor.isObject()) {
                continue;
            }
            if (sensor.path("channelResult").asInt(0) != 0) {
                continue;
            }
            int channel = sensor.path("channel").asInt(-1);
            String nodecode = decoded.path("nodeId").asText("");
            if (channel < 0) {
                continue;
            }
            SensorMetadata meta = registry.resolve(message.getDeviceId(), nodecode, channel);
            String metric = metricKey(meta.getSensorType());
            if (metric == null) {
                continue;
            }
            double value = sensor.path("sensorValue").asDouble(Double.NaN);
            if (Double.isNaN(value)) {
                continue;
            }
            try {
                // 新规则 metricDeviceId 存传感器编码；旧规则存上报设备编码（stationcode），两者都评估以兼容
                Set<String> metricIds = new LinkedHashSet<>();
                metricIds.add(message.getDeviceId());
                String sensorCode = meta.getSensorCode();
                if (sensorCode != null && !sensorCode.trim().isEmpty()) {
                    metricIds.add(sensorCode.trim());
                }
                for (String metricId : metricIds) {
                    ruleEngine.evaluateCondition(metricId, metric, value);
                }
            } catch (RuntimeException e) {
                log.warn("condition rule evaluate failed: device={} metric={} err={}",
                        message.getDeviceId(), metric, e.getMessage());
            }
        }
    }

    /** sensorType → 条件规则指标键；未映射类型返回 null。 */
    public static String metricKey(String sensorType) {
        if (sensorType == null) {
            return null;
        }
        switch (sensorType.trim()) {
            case "1": return "temp";
            case "2": return "humid";
            case "3": return "press";
            default: return null;
        }
    }
}
