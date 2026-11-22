package com.iot.platform.service.control;

import com.iot.platform.mq.DownlinkMessage;
import com.iot.platform.repository.ControlLogRepository;
import com.iot.platform.service.DownlinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.platform.model.ControlLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 控制下发执行 + ControlLog 状态机：
 * <ul>
 *   <li>下发：组装 0x19 网关设备控制指令走 {@link DownlinkService} 真实链路（dtm → ProxyTcp → 网关）；</li>
 *   <li>PENDING：指令已投递 MQ，等待设备 0x1A 回报；</li>
 *   <li>TIMEOUT：由 {@link ControlLogStateMachine} 定时扫描判定（超时未回报）；</li>
 *   <li>FAILED：下发同步失败（MQ 不可用等）直接落败；</li>
 *   <li>SUCCESS / FAILED（回报）：由 ControlFeedbackHandler 按 0x1A 更新。</li>
 * </ul>
 */
@Service
public class ControlExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ControlExecutionService.class);

    public static final String RESULT_PENDING = "PENDING";
    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_TIMEOUT = "TIMEOUT";
    public static final String RESULT_FAILED = "FAILED";
    public static final String RESULT_PARTIAL = "PARTIAL";

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_RULE = "RULE";
    public static final String SOURCE_GROUP = "GROUP";

    /**
     * 0x19 devices[].action 操作码（协议假设约定）：
     * 1=开关（value 1 开 / 0 关），2=停止，3=开度（value 0-100%）。
     */
    public static final int OP_SWITCH = 1;
    public static final int OP_STOP = 2;
    public static final int OP_LEVEL = 3;

    /** 0x1A controlResponse / channelResult：0 视为成功，非 0 失败。 */
    public static final int FEEDBACK_OK = 0;

    private final DownlinkService downlinkService;
    private final ControlLogRepository controlLogRepository;
    private final ObjectMapper objectMapper;

    public ControlExecutionService(DownlinkService downlinkService,
                                   ControlLogRepository controlLogRepository,
                                   ObjectMapper objectMapper) {
        this.downlinkService = downlinkService;
        this.controlLogRepository = controlLogRepository;
        this.objectMapper = objectMapper;
    }

    @Value("${app.control.timeout-seconds:120}")
    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    private long timeoutSeconds = 120;

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * 对单台受控设备下发动作并落 PENDING 控制记录。
     *
     * @param action ON / OFF / OPEN / CLOSE / STOP / LEVEL
     */
    public ControlLog executeDevice(DeviceEntry entry, String action, String actionValue,
                                    String ruleId, String ruleName, String source,
                                    String plotId, String orgCode) {
        ControlLog clog = new ControlLog();
        clog.setId(UUID.randomUUID().toString());
        clog.setRuleId(ruleId);
        clog.setRuleName(ruleName);
        clog.setDeviceId(entry.getDeviceId());
        clog.setNodeId(entry.getNodeId());
        clog.setChannel(entry.getChannel());
        clog.setDeviceName(entry.getDeviceName());
        clog.setSource(source);
        clog.setPlotId(plotId);
        clog.setAction(action);
        clog.setActionValue(actionValue);
        clog.setOrgCode(orgCode);
        clog.setCreatedAt(new Date());
        try {
            DownlinkMessage message = buildControlMessage(entry, action, actionValue);
            downlinkService.send(message);
            clog.setResult(RESULT_PENDING);
            clog.setDetail("指令已发送，等待设备回报");
        } catch (RuntimeException e) {
            log.warn("control downlink failed: device={} action={} err={}", entry.getDeviceId(), action, e.getMessage());
            clog.setResult(RESULT_FAILED);
            clog.setDetail("指令未下发：" + e.getMessage());
            clog.setFinishedAt(new Date());
        }
        return controlLogRepository.save(clog);
    }

    /** 组装 0x19 网关设备控制 DownlinkMessage（serviceType=25，结构化 serviceDataDecoded）。 */
    public DownlinkMessage buildControlMessage(DeviceEntry entry, String action, String actionValue) {
        int op;
        int value;
        switch (action) {
            case "ON":
            case "OPEN":
                op = OP_SWITCH;
                value = 1;
                break;
            case "OFF":
            case "CLOSE":
                op = OP_SWITCH;
                value = 0;
                break;
            case "STOP":
                op = OP_STOP;
                value = 0;
                break;
            case "LEVEL":
                op = OP_LEVEL;
                value = parsePercent(actionValue);
                break;
            default:
                throw new IllegalArgumentException("unsupported control action: " + action);
        }
        ObjectNode decoded = objectMapper.createObjectNode();
        decoded.put("nodeId", entry.getNodeId());
        decoded.put("deviceSize", 1);
        ArrayNode devices = decoded.putArray("devices");
        ObjectNode d = devices.addObject();
        d.put("channel", entry.getChannel() == null ? 0 : entry.getChannel());
        d.put("action", op);
        d.put("value", value);

        DownlinkMessage message = new DownlinkMessage();
        message.setDeviceId(entry.getDeviceId());
        message.setNodeId(entry.getNodeId());
        message.setServiceType(25);
        message.setServiceVersion(1);
        message.setServiceDataEncoding(2);
        message.setServiceDataProtocolVersion(1);
        message.setServiceEncryption(0);
        message.setServiceDataDecoded(decoded);
        return message;
    }

    /** 解析设备列表 JSON；格式非法时返回空列表（不允许下发）。 */
    public List<DeviceEntry> parseDevices(String devicesJson) {
        if (devicesJson == null || devicesJson.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(devicesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DeviceEntry.class));
        } catch (Exception e) {
            log.warn("invalid control rule devices json: {}", e.getMessage());
            return List.of();
        }
    }

    private static int parsePercent(String actionValue) {
        if (actionValue == null || actionValue.isEmpty()) {
            return 0;
        }
        try {
            int v = (int) Double.parseDouble(actionValue);
            return Math.max(0, Math.min(100, v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
