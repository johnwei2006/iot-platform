package com.iot.dtm.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * 平台-网关协议 5.0 的全部 37 个 serviceType（Protocal5.0.xlsx「平台-网关协议」）。
 * 与 resources/protocol-schemas.json 一一对应，由 ProtocolRegistry 启动时校验。
 */
public enum ServiceType {

    GATEWAY_HEART_BEAT(1, "GATEWAY_HEART_BEAT", "uplink", "uplink", true, false),
    GATEWAY_TIME_SYNC_RESPONSE(3, "GATEWAY_TIME_SYNC_RESPONSE", "downlink", "downlink", false, false),
    GATEWAY_TIME_SYNC_REQUEST(4, "GATEWAY_TIME_SYNC_REQUEST", "uplink_callback", "uplink_callback", true, false),
    GATEWAY_INFO_QUERY(5, "GATEWAY_INFO_QUERY", "downlink_callback", "downlink_callback", true, false),
    GATEWAY_INFO_REPORT(6, "GATEWAY_INFO_REPORT", "uplink", "uplink", false, false),
    GATEWAY_CONFIG_QUERY(7, "GATEWAY_CONFIG_QUERY", "downlink_callback", "downlink_callback", true, false),
    GATEWAY_CONFIG_REPORT(8, "GATEWAY_CONFIG_REPORT", "uplink", "uplink", false, false),
    GATEWAY_CONFIG(9, "GATEWAY_CONFIG", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_CONFIG_RESPONSE(10, "GATEWAY_CONFIG_RESPONSE", "uplink", "uplink", false, false),
    GATEWAY_CONFIG_NODE_QUERY(11, "GATEWAY_CONFIG_NODE_QUERY", "downlink_callback", "downlink_callback", true, false),
    GATEWAY_CONFIG_NODE_REPORT(12, "GATEWAY_CONFIG_NODE_REPORT", "uplink", "uplink", false, false),
    GATEWAY_CONFIG_NODE(13, "GATEWAY_CONFIG_NODE", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_CONFIG_NODE_RESPONSE(14, "GATEWAY_CONFIG_NODE_RESPONSE", "uplink", "uplink", false, false),
    GATEWAY_CONFIG_SENSOR_QUERY(15, "GATEWAY_CONFIG_SENSOR_QUERY", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_CONFIG_SENSOR_REPORT(16, "GATEWAY_CONFIG_SENSOR_REPORT", "uplink", "uplink", false, false),
    GATEWAY_CONFIG_SENSOR(17, "GATEWAY_CONFIG_SENSOR", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_CONFIG_SENSOR_RESPONSE(18, "GATEWAY_CONFIG_SENSOR_RESPONSE", "uplink", "uplink", false, false),
    GATEWAY_CONFIG_DEVICE_QUERY(19, "GATEWAY_CONFIG_DEVICE_QUERY", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_CONFIG_DEVICE_REPORT(20, "GATEWAY_CONFIG_DEVICE_REPORT", "uplink", "uplink", false, false),
    GATEWAY_CONFIG_DEVICE(21, "GATEWAY_CONFIG_DEVICE", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_CONFIG_DEVICE_RESPONSE(22, "GATEWAY_CONFIG_DEVICE_RESPONSE", "uplink", "uplink", false, false),
    GATEWAY_SENSOR_DATA_UPLOAD_NOTICE(23, "GATEWAY_SENSOR_DATA_UPLOAD_NOTICE", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_SENSOR_DATA_REPORT(24, "GATEWAY_SENSOR_DATA_REPORT", "uplink", "uplink", false, false),
    GATEWAY_DEVICE_CONTROL(25, "GATEWAY_DEVICE_CONTROL", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_DEVICE_CONTROL_RESPONSE(26, "GATEWAY_DEVICE_CONTROL_RESPONSE", "uplink", "uplink", false, false),
    GATEWAY_DEVICE_CONTROL_BATCH(27, "GATEWAY_DEVICE_CONTROL_BATCH", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_DEVICE_CONTROL_BATCH_RESPONSE(28, "GATEWAY_DEVICE_CONTROL_BATCH_RESPONSE", "uplink", "uplink", false, false),
    GATEWAY_CONFIG_TASK_QUERY(33, "GATEWAY_CONFIG_TASK_QUERY", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_CONFIG_TASK_REPORT(34, "GATEWAY_CONFIG_TASK_REPORT", "uplink", "uplink", false, false),
    GATEWAY_CONFIG_TASK(35, "GATEWAY_CONFIG_TASK", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_CONFIG_TASK_RESPONSE(36, "GATEWAY_CONFIG_TASK_RESPONSE", "uplink", "uplink", false, false),
    GATEWAY_CONFIG_TASK_LIST(37, "GATEWAY_CONFIG_TASK_LIST", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_CONFIG_TASK_LIST_RESPONSE(38, "GATEWAY_CONFIG_TASK_LIST_RESPONSE", "uplink", "uplink", false, false),
    GATEWAY_UPGRADE(92, "GATEWAY_UPGRADE", "downlink_callback", "downlink_callback", false, false),
    GATEWAY_UPGRADE_STATUS(93, "GATEWAY_UPGRADE_STATUS", "uplink", "uplink", false, false),
    GATEWAY_GNSS(96, "GATEWAY_GNSS", "uplink", "uplink", false, true),
    GATEWAY_REBOOT(101, "GATEWAY_REBOOT", "downlink", "downlink", false, false);

    private final int code;
    private final String protocolName;
    private final String direction;
    private final String transactionType;
    private final boolean empty;
    private final boolean unsupported;

    ServiceType(int code, String protocolName, String direction, String transactionType,
                boolean empty, boolean unsupported) {
        this.code = code;
        this.protocolName = protocolName;
        this.direction = direction;
        this.transactionType = transactionType;
        this.empty = empty;
        this.unsupported = unsupported;
    }

    private static final Map<Integer, ServiceType> BY_CODE = new HashMap<Integer, ServiceType>();
    static {
        for (ServiceType t : values()) {
            BY_CODE.put(t.code, t);
        }
    }

    public static ServiceType of(int code) {
        return BY_CODE.get(code);
    }

    public int getCode() { return code; }
    public String getProtocolName() { return protocolName; }
    public String getDirection() { return direction; }
    public String getTransactionType() { return transactionType; }
    public boolean isEmpty() { return empty; }
    public boolean isUnsupported() { return unsupported; }
}
