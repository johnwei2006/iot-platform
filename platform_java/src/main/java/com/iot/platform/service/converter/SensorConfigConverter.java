package com.iot.platform.service.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.model.Node;
import com.iot.platform.model.Sensor;
import com.iot.platform.repository.NodeRepository;
import com.iot.platform.repository.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

/**
 * 传感器配置报文转换器：0X10 GATEWAY_CONFIG_SENSOR_REPORT(16)、0X11 GATEWAY_CONFIG_SENSOR(17)。
 *
 * <p>schema 的 sensors 数组含 channel / sensorType / sensorCode，这里：
 * 1) 登记内存缓存 (stationcode, nodecode, channel) -> (sensorCode, sensorType) 供 0X18 快速补全；
 * 2) 落库 iot_sensor（按 sensorCode upsert），节点不存在时自动建档 iot_node，
 *    使三层架构（网关→节点→传感器）在采集链路中自愈补全，重启后配置不丢。
 */
@Component
public class SensorConfigConverter implements UplinkBusinessConverter {

    public static final int SERVICE_CONFIG_SENSOR_REPORT = 0x10;
    public static final int SERVICE_CONFIG_SENSOR = 0x11;

    private static final Logger log = LoggerFactory.getLogger(SensorConfigConverter.class);

    private final SensorMetadataRegistry registry;
    private final SensorRepository sensorRepository;
    private final NodeRepository nodeRepository;

    @Autowired
    public SensorConfigConverter(SensorMetadataRegistry registry,
                                 SensorRepository sensorRepository,
                                 NodeRepository nodeRepository) {
        this.registry = registry;
        this.sensorRepository = sensorRepository;
        this.nodeRepository = nodeRepository;
    }

    @Override
    public boolean supports(int serviceType) {
        return serviceType == SERVICE_CONFIG_SENSOR_REPORT || serviceType == SERVICE_CONFIG_SENSOR;
    }

    @Override
    public ConversionResult convert(UplinkMessage message) {
        JsonNode decoded = message.getServiceDataDecoded();
        if (decoded == null || !decoded.isObject()) {
            return ConversionResult.success(0, 1);
        }
        JsonNode sensors = decoded.get("sensors");
        if (sensors == null || !sensors.isArray()) {
            log.debug("sensor config without sensors array: deviceId={} serviceType={}",
                    message.getDeviceId(), message.getServiceType());
            return ConversionResult.success(0, 1);
        }

        String stationcode = message.getDeviceId();
        String nodecode = JsonNodeUtils.text(decoded.get("nodeId"));
        if (nodecode.isEmpty()) {
            nodecode = JsonNodeUtils.text(message.getNodeId());
        }

        Node node = sensorRepository == null ? null : ensureNode(stationcode, nodecode);
        int processed = 0;
        int skipped = 0;
        for (JsonNode sensor : sensors) {
            if (sensor == null || !sensor.isObject()) {
                skipped++;
                continue;
            }
            int channel = sensor.path("channel").asInt(-1);
            if (channel < 0) {
                skipped++;
                continue;
            }
            String sensorType = JsonNodeUtils.text(sensor.get("sensorType"));
            String sensorCode = JsonNodeUtils.text(sensor.get("sensorCode"));
            if (sensorCode.isEmpty()) {
                sensorCode = "CH" + channel;
            }
            registry.register(stationcode, nodecode, channel, sensorCode, sensorType);
            if (sensorRepository != null) {
                upsertSensor(sensorCode, sensorType, channel, node);
            }
            processed++;
        }
        log.debug("sensor config applied: deviceId={} nodecode={} registered={} skipped={}",
                stationcode, nodecode, processed, skipped);
        return ConversionResult.success(processed, skipped);
    }

    /** 节点不存在时按上报信息自动建档（挂在当前网关下）。 */
    private Node ensureNode(String gatewayCode, String nodeCode) {
        if (nodeCode == null || nodeCode.isEmpty()) {
            return null;
        }
        Optional<Node> existing = nodeRepository.findByNodeCode(nodeCode);
        if (existing.isPresent()) {
            return existing.get();
        }
        Node node = new Node();
        node.setNodeCode(nodeCode);
        node.setNodeName(nodeCode);
        node.setGatewayCode(gatewayCode);
        node.setCreateTime(new Date());
        node.setUpdateTime(new Date());
        node.setIsDeleted(0);
        node.setOnline(1);
        return nodeRepository.save(node);
    }

    /** 按 sensorCode upsert 传感器配置。 */
    private void upsertSensor(String sensorCode, String sensorType, int channel, Node node) {
        Date now = new Date();
        Sensor s = sensorRepository.findBySensorCode(sensorCode).orElseGet(() -> {
            Sensor created = new Sensor();
            created.setSensorCode(sensorCode);
            created.setSensorName(sensorCode);
            created.setCreateTime(now);
            created.setCreateBy("uplink:0x10");
            created.setIsDeleted(0);
            return created;
        });
        s.setSensorClassType(sensorType);
        s.setChannel(String.valueOf(channel));
        if (node != null) {
            s.setNodeCode(node.getNodeCode());
            s.setOrgCode(node.getOrgCode());
            s.setOwnerUserId(node.getOwnerUserId());
        }
        s.setUpdateTime(now);
        s.setUpdateBy("uplink:0x10");
        sensorRepository.save(s);
    }
}
