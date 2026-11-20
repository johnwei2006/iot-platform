package com.iot.platform.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 上行消息接入层：MQ 传输边界。
 *
 * <p>只负责把 AMQP Message 反序列化为 {@link UplinkMessage}，记录一条接入日志，
 * 再交给同包队列层 {@link UplinkProcessingService} 编排处理；业务处理、落库、缓存均不在此类。
 * 反序列化或编排失败时抛 RuntimeException，保持 RabbitMQ 重试 / 死信语义。
 */
@Component
public class UplinkMessageListener {

    private static final Logger log = LoggerFactory.getLogger(UplinkMessageListener.class);

    private final ObjectMapper objectMapper;
    private final UplinkProcessingService processingService;

    @Autowired
    public UplinkMessageListener(ObjectMapper objectMapper, UplinkProcessingService processingService) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }

    @RabbitListener(queues = "${app.mq.platform-uplink}")
    public void onUplink(Message message) {
        try {
            UplinkMessage up = objectMapper.readValue(message.getBody(), UplinkMessage.class);
            log.info("ingested uplink from {} serviceType={} protocol={} decodeStatus={} transactionType={}",
                    up.getDeviceId(), up.getServiceType(),
                    up.getProtocolName() == null ? "-" : up.getProtocolName(),
                    up.getDecodeStatus() == null ? "-" : up.getDecodeStatus(),
                    up.getTransactionType() == null ? "-" : up.getTransactionType());
            processingService.process(up);
        } catch (Exception e) {
            log.error("failed to ingest uplink", e);
            throw new RuntimeException(e);
        }
    }
}
