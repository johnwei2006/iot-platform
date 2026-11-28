package com.iot.platform.service.uplink;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.constant.GatewayProtocolConstants;
import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.model.Gateway;
import com.iot.platform.model.GatewayCommandLog;
import com.iot.platform.model.GatewayConfigSnapshot;
import com.iot.platform.model.GatewayInfoHistory;
import com.iot.platform.model.GatewayTask;
import com.iot.platform.repository.GatewayCommandLogRepository;
import com.iot.platform.repository.GatewayConfigSnapshotRepository;
import com.iot.platform.repository.GatewayInfoHistoryRepository;
import com.iot.platform.repository.GatewayRepository;
import com.iot.platform.repository.GatewayTaskRepository;
import com.iot.platform.service.gateway.GatewayConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/** 网关配置上报/反馈统一落库：旁路步骤，异常不阻断主链路。 */
@Component
@Order(7)
public class GatewayConfigUplinkHandler implements UplinkHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayConfigUplinkHandler.class);

    private final GatewayConfigSnapshotRepository snapshotRepository;
    private final GatewayCommandLogRepository commandLogRepository;
    private final GatewayTaskRepository taskRepository;
    private final GatewayRepository gatewayRepository;
    private final GatewayInfoHistoryRepository infoHistoryRepository;
    private final GatewayConfigService configService;
    private final ObjectMapper mapper = new ObjectMapper();

    public GatewayConfigUplinkHandler(GatewayConfigSnapshotRepository snapshotRepository,
                                      GatewayCommandLogRepository commandLogRepository,
                                      GatewayTaskRepository taskRepository,
                                      GatewayRepository gatewayRepository,
                                      GatewayInfoHistoryRepository infoHistoryRepository,
                                      GatewayConfigService configService) {
        this.snapshotRepository = snapshotRepository;
        this.commandLogRepository = commandLogRepository;
        this.taskRepository = taskRepository;
        this.gatewayRepository = gatewayRepository;
        this.infoHistoryRepository = infoHistoryRepository;
        this.configService = configService;
    }

    @Override
    public String name() { return "gateway-config-uplink"; }

    @Override
    public boolean critical() { return false; }

    @Override
    public void handle(UplinkMessage message) {
        if (message == null) return;
        int type = message.getServiceType();
        String category = categoryOf(type);
        if (category == null) return;
        JsonNode decoded = message.getServiceDataDecoded();
        String payload = decoded == null ? "{}" : decoded.toString();
        long ts = message.getTimestamp();
        Date reportedAt = ts > 0 ? new Date(ts) : new Date();
        if (type == GatewayProtocolConstants.GATEWAY_CONFIG_REPORT) {
            configService.applyUpstream(message.getDeviceId(), decoded, reportedAt);
        } else {
            GatewayConfigSnapshot snapshot = new GatewayConfigSnapshot();
            snapshot.setId(UUID.randomUUID().toString());
            snapshot.setGatewayCode(message.getDeviceId());
            snapshot.setServiceType(type);
            snapshot.setCategory(category);
            snapshot.setPayload(payload);
            snapshot.setReportedAt(reportedAt);
            snapshot.setCreatedAt(new Date());
            snapshotRepository.save(snapshot);
        }

        if (type == GatewayProtocolConstants.GATEWAY_INFO_REPORT) {
            applyGatewayInfo(message.getDeviceId(), decoded, reportedAt, payload);
        }
        if (type == GatewayProtocolConstants.GATEWAY_CONFIG_TASK_LIST_RESPONSE) {
            applyTaskFeedback(message.getDeviceId(), decoded);
        } else {
            markCommandFeedback(message.getDeviceId(), type, decoded);
        }
    }

    /** 0x06 网关信息上报：更新网关最新信息并追加一条上报历史。 */
    private void applyGatewayInfo(String gatewayCode, JsonNode decoded, Date reportedAt, String payload) {
        if (decoded == null || !decoded.isObject()) return;
        Gateway gateway = gatewayRepository.findByDeviceId(gatewayCode).orElse(null);
        if (gateway != null) {
            copyInfoFields(decoded, gateway);
            gateway.setInfoReportedAt(reportedAt);
            gatewayRepository.save(gateway);
        } else {
            log.warn("0x06 info report for unknown gateway {}, skip iot_gateway update", gatewayCode);
        }
        GatewayInfoHistory history = new GatewayInfoHistory();
        history.setId(UUID.randomUUID().toString());
        history.setGatewayCode(gatewayCode);
        copyInfoFields(decoded, history);
        history.setPayload(payload);
        history.setReportedAt(reportedAt);
        history.setCreatedAt(new Date());
        infoHistoryRepository.save(history);
    }

    private static void copyInfoFields(JsonNode src, Gateway dst) {
        dst.setConfigVersion(intOf(src, "configVersion"));
        dst.setGatewayId(textOf(src, "gatewayId"));
        dst.setProjectId(textOf(src, "projectId"));
        dst.setHeartbeatInterval(intOf(src, "heartbeatInterval"));
        dst.setReportInterval(intOf(src, "reportInterval"));
        dst.setRtcTime(textOf(src, "rtcTime"));
        dst.setSystemServerUrl(textOf(src, "systemServerUrl"));
        dst.setSystemServerPort(intOf(src, "systemServerPort"));
        dst.setHardwareVersion(textOf(src, "hardwareVersion"));
        dst.setFirmwareVersion(textOf(src, "firmwareVersion"));
        dst.setGatewayMcuid(textOf(src, "gatewayMcuid"));
        dst.setMemoryTotal(intOf(src, "memoryTotal"));
        dst.setMemoryUsed(intOf(src, "memoryUsed"));
        dst.setFlashTotal(intOf(src, "flashTotal"));
        dst.setFlashUsed(intOf(src, "flashUsed"));
        dst.setUplinkMessageSize(intOf(src, "uplinkMessageSize"));
        dst.setDownlinkMessageSize(intOf(src, "downlinkMessageSize"));
        dst.setBattery(textOf(src, "battery"));
        dst.setAdapterExists(intOf(src, "adapterExists"));
        dst.setChargingEnabled(intOf(src, "chargingEnabled"));
        dst.setLatestRebootTime(textOf(src, "latestRebootTime"));
    }

    private static void copyInfoFields(JsonNode src, GatewayInfoHistory dst) {
        dst.setConfigVersion(intOf(src, "configVersion"));
        dst.setGatewayId(textOf(src, "gatewayId"));
        dst.setProjectId(textOf(src, "projectId"));
        dst.setHeartbeatInterval(intOf(src, "heartbeatInterval"));
        dst.setReportInterval(intOf(src, "reportInterval"));
        dst.setRtcTime(textOf(src, "rtcTime"));
        dst.setSystemServerUrl(textOf(src, "systemServerUrl"));
        dst.setSystemServerPort(intOf(src, "systemServerPort"));
        dst.setHardwareVersion(textOf(src, "hardwareVersion"));
        dst.setFirmwareVersion(textOf(src, "firmwareVersion"));
        dst.setGatewayMcuid(textOf(src, "gatewayMcuid"));
        dst.setMemoryTotal(intOf(src, "memoryTotal"));
        dst.setMemoryUsed(intOf(src, "memoryUsed"));
        dst.setFlashTotal(intOf(src, "flashTotal"));
        dst.setFlashUsed(intOf(src, "flashUsed"));
        dst.setUplinkMessageSize(intOf(src, "uplinkMessageSize"));
        dst.setDownlinkMessageSize(intOf(src, "downlinkMessageSize"));
        dst.setBattery(textOf(src, "battery"));
        dst.setAdapterExists(intOf(src, "adapterExists"));
        dst.setChargingEnabled(intOf(src, "chargingEnabled"));
        dst.setLatestRebootTime(textOf(src, "latestRebootTime"));
    }

    private static Integer intOf(JsonNode src, String key) {
        JsonNode value = src.get(key);
        if (value == null || value.isNull()) return null;
        if (value.isNumber() || value.isBoolean()) return value.asInt();
        String text = value.asText().trim();
        if (text.isEmpty()) return null;
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String textOf(JsonNode src, String key) {
        JsonNode value = src.get(key);
        if (value == null || value.isNull()) return null;
        String text = value.isValueNode() ? value.asText() : value.toString();
        return text.isEmpty() ? null : text;
    }

    private void markCommandFeedback(String gatewayCode, int serviceType, JsonNode decoded) {
        var q = Wrappers.<GatewayCommandLog>lambdaQuery()
                .eq(GatewayCommandLog::getGatewayCode, gatewayCode)
                .eq(GatewayCommandLog::getServiceType, serviceType - 1)
                .eq(GatewayCommandLog::getStatus, GatewayCommandLog.PENDING)
                .orderByDesc(GatewayCommandLog::getCreatedAt)
                .last("LIMIT 1");
        GatewayCommandLog pending = commandLogRepository.findAll(q).stream().findFirst().orElse(null);
        if (pending == null) return;
        pending.setResult(resultOf(decoded));
        pending.setReason(reasonOf(decoded));
        pending.setStatus(resultOf(decoded) != null && resultOf(decoded).contains("FAIL")
                ? GatewayCommandLog.FAILED : GatewayCommandLog.SUCCESS);
        pending.setFinishedAt(new Date());
        commandLogRepository.save(pending);
        if (GatewayCommandLog.FAILED.equals(pending.getStatus())
                && pending.getServiceType() == GatewayProtocolConstants.GATEWAY_CONFIG) {
            configService.markFailed(gatewayCode);
        }
    }

    private void applyTaskFeedback(String gatewayCode, JsonNode decoded) {
        if (decoded == null || !decoded.isObject()) return;
        JsonNode tasks = decoded.path("tasks");
        if (!tasks.isArray()) return;
        for (JsonNode task : tasks) {
            int taskNo = task.path("taskId").asInt();
            GatewayTask entity = taskRepository.findAll(Wrappers.<GatewayTask>lambdaQuery()
                    .eq(GatewayTask::getGatewayCode, gatewayCode)
                    .eq(GatewayTask::getTaskNo, taskNo)).stream().findFirst().orElse(null);
            if (entity == null) continue;
            entity.setFeedbackMsg(task.path("message").asText(""));
            entity.setDeployStatus(task.path("result").asInt(0) == 0 ? "SUCCESS" : "FAILED");
            taskRepository.save(entity);
        }
    }

    private String resultOf(JsonNode decoded) {
        if (decoded == null || !decoded.has("result")) return null;
        return decoded.get("result").asText();
    }

    private String reasonOf(JsonNode decoded) {
        if (decoded == null) return null;
        String msg = decoded.path("message").asText("");
        return msg.length() > 255 ? msg.substring(0, 255) : msg;
    }

    private String categoryOf(int type) {
        switch (type) {
            case GatewayProtocolConstants.GATEWAY_INFO_REPORT: return "INFO";
            case GatewayProtocolConstants.GATEWAY_CONFIG_REPORT: return "CONFIG";
            case GatewayProtocolConstants.GATEWAY_CONFIG_NODE_REPORT: return "NODE";
            case GatewayProtocolConstants.GATEWAY_CONFIG_NODE_RESPONSE: return "NODE_RESPONSE";
            case GatewayProtocolConstants.GATEWAY_CONFIG_SENSOR_REPORT: return "SENSOR";
            case GatewayProtocolConstants.GATEWAY_CONFIG_DEVICE_REPORT: return "DEVICE";
            case GatewayProtocolConstants.GATEWAY_CONFIG_TASK_LIST_RESPONSE: return "TASK";
            default: return null;
        }
    }
}
