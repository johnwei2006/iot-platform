package com.iot.platform.constant;

/** 网关管理服务类型常量；与 dtm ServiceType 保持一致，0x65 在 Java 枚举为 101。 */
public final class GatewayProtocolConstants {

    public static final int GATEWAY_INFO_REPORT = 6;
    public static final int GATEWAY_CONFIG_REPORT = 8;
    public static final int GATEWAY_CONFIG = 9;
    public static final int GATEWAY_CONFIG_NODE_REPORT = 12;
    public static final int GATEWAY_CONFIG_NODE = 13;
    public static final int GATEWAY_CONFIG_NODE_RESPONSE = 14;
    public static final int GATEWAY_CONFIG_SENSOR_REPORT = 16;
    public static final int GATEWAY_CONFIG_SENSOR = 17;
    public static final int GATEWAY_CONFIG_DEVICE_REPORT = 20;
    public static final int GATEWAY_CONFIG_DEVICE = 21;
    public static final int GATEWAY_CONFIG_TASK_LIST = 37;
    public static final int GATEWAY_CONFIG_TASK_LIST_RESPONSE = 38;
    public static final int GATEWAY_REBOOT = 101;

    private GatewayProtocolConstants() { }
}
