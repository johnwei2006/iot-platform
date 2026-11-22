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

    private final MetricThresholdRepository thresholdRepository;
    private final AlarmRepository alarmRepository;
    private final SensorMetadataRegistry registry;
    private final com.iot.platform.repository.MessageRepository messageRepository;

    public ThresholdAlarmHandler(MetricThresholdRepository thresholdRepository,
                                 AlarmRepository alarmRepository,
                                 SensorMetadataRegistry registry,
                                 com.iot.platform.repository.MessageRepository messageRepository) {
        this.thresholdRepository = thresholdRepository;
        this.alarmRepository = alarmRepository;
        this.registry = registry;
        this.messageRepository = messageRepository;
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
}