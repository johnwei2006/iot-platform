package com.iot.dtm.mq;

import com.iot.dtm.config.WorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 下游发布器：发送后等待 broker publisher confirm。
 * confirm ACK 才返回成功，由 Consumer 再对上游消息 ACK，保证「至少一次」。
 */
@Component
public class MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(MessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final WorkerProperties properties;

    @Autowired
    public MessagePublisher(RabbitTemplate rabbitTemplate, WorkerProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publish(String exchange, String routingKey, Message message) {
        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.send(exchange, routingKey, message, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(properties.getConfirmTimeoutMs(), TimeUnit.MILLISECONDS);
            if (confirm == null || !confirm.isAck()) {
                throw new AmqpException("publisher confirm nack: "
                        + (confirm == null ? "no confirm" : confirm.getReason()));
            }
            log.debug("published {} bytes to {}/{} (confirm={})", message.getBody().length,
                    exchange, routingKey, correlation.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmqpException("interrupted while waiting publisher confirm", e);
        } catch (TimeoutException e) {
            throw new AmqpException("publisher confirm timeout after "
                    + properties.getConfirmTimeoutMs() + "ms", e);
        } catch (ExecutionException e) {
            throw new AmqpException("publisher confirm failed: " + e.getCause(), e.getCause());
        }
    }
}
