package com.iot.platform.service.uplink;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.Alarm;
import com.iot.platform.model.MetricThreshold;
import com.iot.platform.model.SensorMetadata;
import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.repository.AlarmRepository;
import com.iot.platform.repository.MetricThresholdRepository;
import com.iot.platform.service.ThresholdService;
import com.iot.platform.service.converter.SensorMetadataRegistry;
import com.iot.platform.service.control.ConditionRuleEvaluateHandler;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 测点阈值超限联动告警：0x18 读数入库后按 iot_metric_threshold 判定，
 * 超限且无未恢复同因告警时落 iot_alarm（OPEN）并给阈值维护人发站内消息。旁路步骤。
 */
@Component
@Order(7)
public class ThresholdAlarmHandler implements UplinkHandler {

    private static final Logger log = LoggerFactory.getLogger(ThresholdAlarmHandler.class);
    private static final String METRIC_ALARM_PREFIX = "threshold:";
    private static final String SENSOR_ALARM_PREFIX = "sensor:";

    private final MetricThresholdRepository thresholdRepository;
    private final AlarmRepository alarmRepository;
    private final SensorMetadataRegistry registry;
    private final com.iot.platform.repository.MessageRepository messageRepository;
    /** 传感器级阈值后备来源（iot_sensor.warn_min/warn_max，优先于网关级阈值） */
    private final com.iot.platform.repository.SensorRepository sensorRepository;
    private final com.iot.platform.repository.NodeRepository nodeRepository;

    public ThresholdAlarmHandler(MetricThresholdRepository thresholdRepository,
                                 AlarmRepository alarmRepository,
                                 SensorMetadataRegistry registry,
                                 com.iot.platform.repository.MessageRepository messageRepository,
                                 @org.springframework.beans.factory.annotation.Autowired(required = false)
                                 com.iot.platform.repository.SensorRepository sensorRepository,
                                 @org.springframework.beans.factory.annotation.Autowired(required = false)
                                 com.iot.platform.repository.NodeRepository nodeRepository) {
        this.thresholdRepository = thresholdRepository;
        this.alarmRepository = alarmRepository;
        this.registry = registry;
        this.messageRepository = messageRepository;
        this.sensorRepository = sensorRepository;
        this.nodeRepository = nodeRepository;
    }

    @Override
    public String name() {
        return "threshold-alarm";
    }

    @Override
    public boolean critical() {
        return false;
    }

    @Override
    public void handle(UplinkMessage message) {
        if (message.getServiceType() != ConditionRuleEvaluateHandler.SERVICE_TYPE_SENSOR_DATA
                || message.getServiceDataDecoded() == null) {
            return;
        }
        JsonNode decoded = message.getServiceDataDecoded();
        JsonNode sensors = decoded.get("sensors");
        if (!decoded.isObject() || sensors == null || !sensors.isArray()) {
            return;
        }
        for (JsonNode sensor : sensors) {
            if (sensor == null || !sensor.isObject() || sensor.path("channelResult").asInt(0) != 0) {
                continue;
            }
            int channel = sensor.path("channel").asInt(-1);
            if (channel < 0) {
                continue;
            }
            SensorMetadata meta = registry.resolve(message.getDeviceId(),
                    decoded.path("nodeId").asText(""), channel);
            String metric = ConditionRuleEvaluateHandler.metricKey(meta.getSensorType());
            if (metric == null) {
                continue;
            }
            double value = sensor.path("sensorValue").asDouble(Double.NaN);
            if (Double.isNaN(value)) {
                continue;
            }
            // 传感器级阈值优先：iot_sensor 配置了 warn_min/warn_max 时不再走网关级 MetricThreshold
            if (raiseSensorAlarmIfExceeds(message.getDeviceId(),
                    decoded.path("nodeId").asText(""), channel, meta.getSensorCode(), value)) {
                continue;
            }
            evaluate(message.getDeviceId(), metric, value);
        }
    }

    public void evaluate(String deviceId, String metric, double value) {
        List<MetricThreshold> thresholds = thresholdRepository.findAll(
                Wrappers.<MetricThreshold>lambdaQuery()
                        .eq(MetricThreshold::getDeviceId, deviceId)
                        .eq(MetricThreshold::getMetric, metric));
        for (MetricThreshold t : thresholds) {
            if (!ThresholdService.exceeds(t, value)) {
                continue;
            }
            String rule = METRIC_ALARM_PREFIX + deviceId + ":" + metric;
            boolean hasOpen = alarmRepository.findAll().stream()
                    .anyMatch(a -> deviceId.equals(a.getDeviceId())
                            && rule.equals(a.getRule())
                            && "OPEN".equals(a.getStatus()));
            if (hasOpen) {
                continue;
            }
            Alarm alarm = new Alarm();
            alarm.setId(UUID.randomUUID().toString());
            alarm.setDeviceId(deviceId);
            alarm.setRule(rule);
            alarm.setLevel("WARN");
            alarm.setMessage(metric + " 超出阈值：当前 " + value
                    + "（下限 " + t.getMinValue() + " / 上限 " + t.getMaxValue() + "）");
            alarm.setStatus("OPEN");
            alarm.setCreatedAt(new Date());
            alarmRepository.save(alarm);
            notifyOwner(t, alarm);
        }
    }

    private void notifyOwner(MetricThreshold t, Alarm alarm) {
        if (t.getUpdatedBy() == null || t.getUpdatedBy().isEmpty()) {
            return;
        }
        com.iot.platform.model.Message m = new com.iot.platform.model.Message();
        m.setId(UUID.randomUUID().toString());
        m.setUserId(t.getUpdatedBy());
        m.setType("ALARM");
        m.setRefId(alarm.getId());
        m.setTitle("指标超限告警");
        m.setContent(alarm.getMessage());
        m.setCreatedAt(new Date());
        messageRepository.save(m);
    }

    /**
     * 传感器级阈值判定：经 iot_node 校验挂靠关系后按 node_code+channel 找 iot_sensor；
     * sensor 配置了 warn_min/warn_max 且超限、且无未恢复同因告警时落 iot_alarm。
     *
     * @return true 表示已按传感器级阈值处理（跳过网关级 MetricThreshold）
     */
    private boolean raiseSensorAlarmIfExceeds(String gatewayCode, String nodeCode,
                                              int channel, String sensorCode, double value) {
        if (sensorRepository == null || nodeRepository == null) {
            return false;
        }
        try {
            com.iot.platform.model.Sensor s = null;
            if (sensorCode != null && !sensorCode.isEmpty() && !sensorCode.startsWith("CH")) {
                s = sensorRepository.findBySensorCode(sensorCode).orElse(null);
            }
            if (s == null && nodeCode != null && !nodeCode.isEmpty()) {
                com.iot.platform.model.Node node = nodeRepository.findByNodeCode(nodeCode).orElse(null);
                if (node != null && node.getGatewayCode() != null
                        && node.getGatewayCode().equals(gatewayCode)) {
                    for (com.iot.platform.model.Sensor cand : sensorRepository.findByNodeCode(nodeCode)) {
                        if (String.valueOf(channel).equals(cand.getChannel())) {
                            s = cand;
                            break;
                        }
                    }
                }
            }
            if (s == null || (s.getWarnMin() == null && s.getWarnMax() == null)) {
                return false;
            }
            boolean below = s.getWarnMin() != null && value < s.getWarnMin();
            boolean above = s.getWarnMax() != null && value > s.getWarnMax();
            if (!below && !above) {
                // 传感器配置了阈值但未超限：视为已处理，跳过网关级判定（优先语义）
                return true;
            }
            String rule = SENSOR_ALARM_PREFIX + s.getSensorCode();
            boolean hasOpen = alarmRepository.findAll().stream()
                    .anyMatch(a -> gatewayCode.equals(a.getDeviceId())
                            && rule.equals(a.getRule())
                            && "OPEN".equals(a.getStatus()));
            if (!hasOpen) {
                Alarm alarm = new Alarm();
                alarm.setId(UUID.randomUUID().toString());
                alarm.setDeviceId(gatewayCode);
                alarm.setRule(rule);
                alarm.setLevel("WARN");
                alarm.setMessage(s.getSensorName() + " 超出阈值：当前 " + value
                        + "（下限 " + s.getWarnMin() + " / 上限 " + s.getWarnMax() + "）");
                alarm.setStatus("OPEN");
                alarm.setCreatedAt(new Date());
                alarmRepository.save(alarm);
            }
            return true;
        } catch (Exception e) {
            log.warn("sensor-level threshold check failed: gateway={} node={} channel={} reason={}",
                    gatewayCode, nodeCode, channel, e.getMessage());
            return false;
        }
    }
}