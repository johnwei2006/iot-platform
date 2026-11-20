package com.iot.dtm.config;

/**
 * RabbitMQ 队列/交换器命名常量。
 * 与 design/api-design.md 中的组件间 MQ 契约保持一致。
 */
public final class MqConstants {

    public static final String EXCHANGE = "iot.exchange";
    public static final String DLX = "iot.dlx";
    public static final String DLQ = "iot.dlq";

    /** 网关（ProxyTcp）上行入口：dtm 从此消费，转发到后台 */
    public static final String PROXY_UPLINK = "proxy.uplink";
    /** 后台（platform_java）上行入口：dtm 转发到此，后台消费 */
    public static final String PLATFORM_UPLINK = "platform.uplink";
    /** 后台下行入口：dtm 从此消费，转发到网关 */
    public static final String PLATFORM_DOWNLINK = "platform.downlink";
    /** 网关下行入口：dtm 转发到此，网关消费 */
    public static final String PROXY_DOWNLINK = "proxy.downlink";

    /** 上行附加的结构化字段（platform.uplink 消息体） */
    public static final String F_SERVICE_DATA_DECODED = "serviceDataDecoded";
    public static final String F_PROTOCOL_NAME = "protocolName";
    public static final String F_TRANSACTION_TYPE = "transactionType";
    public static final String F_DECODE_STATUS = "decodeStatus";
    public static final String F_DECODE_ERROR = "decodeError";

    /** 处理失败时写在 AMQP header 上的状态标记（不改变消息体） */
    public static final String H_DECODE_STATUS = "decodeStatus";
    public static final String H_DECODE_ERROR = "decodeError";
    public static final String H_ENCODE_STATUS = "encodeStatus";
    public static final String H_ENCODE_ERROR = "encodeError";

    private MqConstants() {
    }
}
