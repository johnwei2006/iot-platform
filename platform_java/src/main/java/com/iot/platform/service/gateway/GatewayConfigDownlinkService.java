package com.iot.platform.service.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.constant.GatewayProtocolConstants;
import com.iot.platform.mq.DownlinkMessage;
import com.iot.platform.model.GatewayCommandLog;
import com.iot.platform.repository.gateway.GatewayCommandLogRepository;
import com.iot.platform.service.DownlinkService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/** 网关配置/任务/重启命令下发：复用现有 DownlinkService 链路并记录命令日志。 */
@Service
public class GatewayConfigDownlinkService {

    private static final Logger log = LoggerFactory.getLogger(GatewayConfigDownlinkService.class);

    private final DownlinkService downlinkService;
    private final GatewayCommandLogRepository commandLogRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public GatewayConfigDownlinkService(DownlinkService downlinkService,
                                        GatewayCommandLogRepository commandLogRepository) {
        this.downlinkService = downlinkService;
        this.commandLogRepository = commandLogRepository;
    }

    /** 下发配置命令；requestId 相同视为幂等，直接返回已有记录。 */
    public GatewayCommandLog send(String gatewayCode, String deviceId, int serviceType,
                                  String commandType, Map<String, Object> decoded,
                                  String requestId, String createdBy) {
        if (gatewayCode == null || gatewayCode.isEmpty()) {
            throw new IllegalArgumentException("gatewayCode 不能为空");
        }
        if (decoded == null || decoded.isEmpty()) {
            throw new IllegalArgumentException("serviceDataDecoded 不能为空");
        }
        if (requestId != null && !requestId.isEmpty()) {
            GatewayCommandLog existing = commandLogRepository.findByRequestId(requestId);
            if (existing != null) {
                return existing;
            }
        }
        String json;
        try {
            json = mapper.writeValueAsString(decoded);
        } catch (Exception e) {
            throw new IllegalArgumentException("serviceDataDecoded 不是合法 JSON", e);
        }
        GatewayCommandLog record = new GatewayCommandLog();
        record.setId(UUID.randomUUID().toString());
        record.setGatewayCode(gatewayCode);
        record.setServiceType(serviceType);
        record.setCommandType(commandType);
        record.setRequestId(requestId);
        record.setPayload(json);
        record.setStatus(GatewayCommandLog.PENDING);
        record.setCreatedBy(createdBy);
        record.setCreatedAt(new Date());
        commandLogRepository.save(record);

        DownlinkMessage message = new DownlinkMessage();
        message.setDeviceId(deviceId != null && !deviceId.isEmpty() ? deviceId : gatewayCode);
        message.setServiceType(serviceType);
        message.setServiceDataDecoded(mapper.valueToTree(decoded));
        downlinkService.send(message);
        record.setSentAt(new Date());
        commandLogRepository.save(record);
        log.info("gateway config downlink sent: gateway={} serviceType={} command={} requestId={}",
                gatewayCode, serviceType, commandType, requestId);
        return record;
    }

    public java.util.List<GatewayCommandLog> logs(String gatewayCode, Integer serviceType, int limit) {
        var q = Wrappers.<GatewayCommandLog>lambdaQuery();
        if (gatewayCode != null && !gatewayCode.isEmpty()) q.eq(GatewayCommandLog::getGatewayCode, gatewayCode);
        if (serviceType != null) q.eq(GatewayCommandLog::getServiceType, serviceType);
        q.orderByDesc(GatewayCommandLog::getCreatedAt).last("LIMIT " + Math.max(1, Math.min(limit, 500)));
        return commandLogRepository.findAll(q);
    }

    /** 网关重启固定参数：appType=1（应用），action=1（重启）。 */
    public GatewayCommandLog reboot(String gatewayCode, String deviceId, int appType, String requestId, String createdBy) {
        return send(gatewayCode, deviceId, GatewayProtocolConstants.GATEWAY_REBOOT, "REBOOT",
                Map.of("appType", appType, "action", 1), requestId, createdBy);
    }
}
