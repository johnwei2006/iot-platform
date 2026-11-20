package com.iot.platform.service.downlink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.mq.DownlinkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 下行步骤 3：序列化并通过 RabbitTemplate 发布到 dtm 的 platform.downlink。
 */
@Component
@Order(3)
public class DownlinkPublishHandler implements DownlinkHandler {

    private static final Logger log = LoggerFactory.getLogger(DownlinkPublishHandler.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchange;
    private final String routingKey;

    @Autowired
    public DownlinkPublishHandler(RabbitTemplate rabbitTemplate,
                                   ObjectMapper objectMapper,
                                   @Value("${app.mq.exchange}") String exchange,
                                   @Value("${app.mq.platform-downlink}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public String name() {
        return "downlinkPublish";
    }

    @Override
    public void handle(DownlinkMessage message) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(message);
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            rabbitTemplate.send(exchange, routingKey, new Message(body, props));
            log.info("sent downlink to device {} serviceType={} exchange={} routingKey={}",
                    message.getDeviceId(), message.getServiceType(), exchange, routingKey);
        } catch (Exception e) {
            log.error("failed to send downlink to device {} serviceType={}",
                    message.getDeviceId(), message.getServiceType(), e);
            throw new RuntimeException(e);
        }
    }
}
