package com.iot.platform.service.control;

import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.service.uplink.UplinkHandler;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.ControlLog;
import com.iot.platform.repository.ControlLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 0x1A 网关设备控制反馈处理：按 deviceId + nodeId + channel 匹配 PENDING 控制记录，
 * 依据 controlResponse / channelResult 迁移 SUCCESS / FAILED。旁路步骤，失败不影响上行 ACK。
 */
@Component
@Order(6)
public class ControlFeedbackHandler implements UplinkHandler {

    private static final Logger log = LoggerFactory.getLogger(ControlFeedbackHandler.class);

    /** dtm 协议：26 (0x1A) 网关设备控制反馈 */
    public static final int SERVICE_TYPE_CONTROL_RESPONSE = 26;

    private final ControlLogRepository controlLogRepository;
    private final ControlLogStateMachine stateMachine;

    public ControlFeedbackHandler(ControlLogRepository controlLogRepository,
                                  ControlLogStateMachine stateMachine) {
        this.controlLogRepository = controlLogRepository;
        this.stateMachine = stateMachine;
    }

    @Override
    public String name() {
        return "control-feedback";
    }

    @Override
    public boolean critical() {
        return false;
    }

    @Override
    public void handle(UplinkMessage message) {
        if (message.getServiceType() != SERVICE_TYPE_CONTROL_RESPONSE
                || message.getServiceDataDecoded() == null) {
            return;
        }
        JsonNode decoded = message.getServiceDataDecoded();
        if (!decoded.isObject()) {
            return;
        }
        String nodeId = decoded.path("nodeId").asText(null);
        JsonNode devices = decoded.path("devices");
        if (nodeId == null || !devices.isArray()) {
            return;
        }
        for (JsonNode device : devices) {
            int channel = device.path("channel").asInt(-1);
            int channelResult = device.path("channelResult").asInt(0);
            List<ControlLog> pending = controlLogRepository.findAll(Wrappers.<ControlLog>lambdaQuery()
                    .eq(ControlLog::getDeviceId, message.getDeviceId())
                    .eq(ControlLog::getNodeId, nodeId)
                    .eq(ControlLog::getChannel, channel)
                    .eq(ControlLog::getResult, ControlExecutionService.RESULT_PENDING));
            for (ControlLog clog : pending) {
                stateMachine.applyFeedback(clog, channelResult);
            }
        }
    }
}
