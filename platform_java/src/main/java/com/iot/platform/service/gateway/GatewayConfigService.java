package com.iot.platform.service.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.constant.GatewayProtocolConstants;
import com.iot.platform.model.GatewayConfigSnapshot;
import com.iot.platform.repository.GatewayConfigSnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 网关配置信息：保存当前生效配置与待下发配置，保证每个网关最多一条未下发/失败记录。 */
@Service
public class GatewayConfigService {

    private final GatewayConfigSnapshotRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public GatewayConfigService(GatewayConfigSnapshotRepository repository) { this.repository = repository; }

    /** 保存待下发配置：若已有未下发或失败记录则复用更新，否则新建。 */
    public GatewayConfigSnapshot savePending(String gatewayCode, Map<String, Object> decoded) {
        if (gatewayCode == null || gatewayCode.isEmpty()) throw new IllegalArgumentException("gatewayCode 不能为空");
        if (decoded == null || decoded.isEmpty()) throw new IllegalArgumentException("配置内容不能为空");
        GatewayConfigSnapshot record = repository.findPendingOrFailed(gatewayCode);
        boolean isNew = record == null;
        if (isNew) {
            record = new GatewayConfigSnapshot();
            record.setId(UUID.randomUUID().toString());
            record.setGatewayCode(gatewayCode);
            record.setServiceType(GatewayProtocolConstants.GATEWAY_CONFIG);
            record.setCategory("CONFIG");
            record.setCreatedAt(new Date());
        }
        applyFields(decoded, record);
        record.setStatus(GatewayConfigSnapshot.STATUS_PENDING);
        record.setConfigTime(new Date());
        record.setSendTime(null);
        record.setPayload(toJson(decoded));
        return isNew ? repository.save(record) : update(record);
    }

    /** 标记配置已下发（写入下发时间）。 */
    public void markSent(String gatewayCode) {
        GatewayConfigSnapshot record = repository.findPendingOrFailed(gatewayCode);
        if (record == null) return;
        if (GatewayConfigSnapshot.STATUS_PENDING.equals(record.getStatus())) record.setSendTime(new Date());
        update(record);
    }

    /** 标记下发失败。 */
    public void markFailed(String gatewayCode) {
        GatewayConfigSnapshot record = repository.findPendingOrFailed(gatewayCode);
        if (record == null) return;
        record.setStatus(GatewayConfigSnapshot.STATUS_FAILED);
        update(record);
    }

    /** 0x08 配置上报：若有待下发/失败记录则确认生效，否则记录网关当前配置。 */
    public GatewayConfigSnapshot applyUpstream(String gatewayCode, JsonNode decoded, Date reportedAt) {
        if (gatewayCode == null || gatewayCode.isEmpty() || decoded == null || !decoded.isObject()) return null;
        GatewayConfigSnapshot record = repository.findPendingOrFailed(gatewayCode);
        boolean isNew = record == null;
        if (isNew) {
            record = new GatewayConfigSnapshot();
            record.setId(UUID.randomUUID().toString());
            record.setGatewayCode(gatewayCode);
            record.setServiceType(GatewayProtocolConstants.GATEWAY_CONFIG_REPORT);
            record.setCategory("CONFIG");
            record.setCreatedAt(new Date());
        }
        applyFields(decoded, record);
        record.setStatus(GatewayConfigSnapshot.STATUS_SUCCESS);
        record.setConfigTime(reportedAt != null ? reportedAt : new Date());
        record.setReportedAt(reportedAt);
        record.setPayload(decoded.toString());
        return isNew ? repository.save(record) : update(record);
    }

    /** 最新一条已生效配置。 */
    public GatewayConfigSnapshot latestSuccess(String gatewayCode) {
        if (gatewayCode == null || gatewayCode.isEmpty()) return null;
        List<GatewayConfigSnapshot> records = repository.findAll(Wrappers.<GatewayConfigSnapshot>lambdaQuery()
                .eq(GatewayConfigSnapshot::getGatewayCode, gatewayCode)
                .eq(GatewayConfigSnapshot::getStatus, GatewayConfigSnapshot.STATUS_SUCCESS)
                .orderByDesc(GatewayConfigSnapshot::getConfigTime)
                .last("LIMIT 1"));
        return records.isEmpty() ? null : records.get(0);
    }

    /** 配置历史列表（按配置时间倒排）。 */
    public List<GatewayConfigSnapshot> list(String gatewayCode, int limit) {
        var q = Wrappers.<GatewayConfigSnapshot>lambdaQuery();
        if (gatewayCode != null && !gatewayCode.isEmpty()) q.eq(GatewayConfigSnapshot::getGatewayCode, gatewayCode);
        q.orderByDesc(GatewayConfigSnapshot::getConfigTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 500)));
        return repository.findAll(q);
    }

    private GatewayConfigSnapshot update(GatewayConfigSnapshot record) {
        repository.update(record);
        return record;
    }

    private void applyFields(JsonNode src, GatewayConfigSnapshot dst) {
        Integer configVersion = intOf(src.path("configVersion"));
        if (configVersion != null) dst.setConfigVersion(configVersion);
        String gatewayId = textOf(src.path("gatewayId"));
        if (gatewayId != null) dst.setGatewayId(gatewayId);
        String projectId = textOf(src.path("projectId"));
        if (projectId != null) dst.setProjectId(projectId);
        String systemServerUrl = textOf(src.path("systemServerUrl"));
        if (systemServerUrl != null) dst.setSystemServerUrl(systemServerUrl);
        Integer systemServerPort = intOf(src.path("systemServerPort"));
        if (systemServerPort != null) dst.setSystemServerPort(systemServerPort);
        Integer heartbeatInterval = intOf(src.path("heartbeatInterval"));
        if (heartbeatInterval != null) dst.setHeartbeatInterval(heartbeatInterval);
        Integer reportInterval = intOf(src.path("reportInterval"));
        if (reportInterval != null) dst.setReportInterval(reportInterval);
        Integer loraFreqGroup = intOf(src.path("loraFreqGroup"));
        if (loraFreqGroup != null) dst.setLoraFreqGroup(loraFreqGroup);
    }

    private void applyFields(Map<String, Object> src, GatewayConfigSnapshot dst) {
        Integer configVersion = intOf(src.get("configVersion"));
        if (configVersion != null) dst.setConfigVersion(configVersion);
        String gatewayId = textOf(src.get("gatewayId"));
        if (gatewayId != null) dst.setGatewayId(gatewayId);
        String projectId = textOf(src.get("projectId"));
        if (projectId != null) dst.setProjectId(projectId);
        String systemServerUrl = textOf(src.get("systemServerUrl"));
        if (systemServerUrl != null) dst.setSystemServerUrl(systemServerUrl);
        Integer systemServerPort = intOf(src.get("systemServerPort"));
        if (systemServerPort != null) dst.setSystemServerPort(systemServerPort);
        Integer heartbeatInterval = intOf(src.get("heartbeatInterval"));
        if (heartbeatInterval != null) dst.setHeartbeatInterval(heartbeatInterval);
        Integer reportInterval = intOf(src.get("reportInterval"));
        if (reportInterval != null) dst.setReportInterval(reportInterval);
        Integer loraFreqGroup = intOf(src.get("loraFreqGroup"));
        if (loraFreqGroup != null) dst.setLoraFreqGroup(loraFreqGroup);
    }

    private static Integer intOf(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isNumber()) return node.asInt();
        return intOf(node.asText());
    }

    private static Integer intOf(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        return intOf(value.toString());
    }

    private static Integer intOf(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String textOf(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String text = node.isValueNode() ? node.asText() : node.toString();
        return text.isEmpty() ? null : text;
    }

    private static String textOf(Object value) {
        if (value == null) return null;
        String text = value.toString();
        return text.isEmpty() ? null : text;
    }

    private String toJson(Map<String, Object> decoded) {
        try {
            return mapper.writeValueAsString(decoded);
        } catch (Exception e) {
            throw new IllegalArgumentException("配置内容不是合法 JSON", e);
        }
    }
}
