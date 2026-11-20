package com.iot.dtm.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.config.MqConstants;
import com.iot.dtm.mq.MessagePublisher;
import com.iot.dtm.profile.DeviceProfileCache;
import com.iot.dtm.protocol.encoder.TransportEncoderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** 上下行 Service 的公共能力：JSON 读写、失败标记、发布确认、下行消息重建。 */
public abstract class AbstractEnvelopeService {

    private static final Logger log = LoggerFactory.getLogger(AbstractEnvelopeService.class);

    protected final ObjectMapper mapper;
    protected final DeviceProfileCache profileCache;
    protected final TransportEncoderManager encoderManager;
    protected final CodecProperties properties;
    protected final MessagePublisher publisher;

    protected AbstractEnvelopeService(DeviceProfileCache profileCache,
                                      TransportEncoderManager encoderManager,
                                      CodecProperties properties,
                                      MessagePublisher publisher) {
        this.profileCache = profileCache;
        this.encoderManager = encoderManager;
        this.properties = properties;
        this.publisher = publisher;
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 下行失败：绝不把无效指令发给网关，直接 NACK 进 DLQ（不受 fail-open 影响）。
     */
    protected AmqpRejectAndDontRequeueException reject(String error, String stage) {
        log.error("dtm {} rejected: {}", stage, error);
        return new AmqpRejectAndDontRequeueException(error);
    }

    /**
     * 业务级失败处理：fail-open 时给原消息打 header 后继续转发；fail-close 时抛出拒绝异常。
     */
    protected Message fail(Message message, String statusHeader, String errorHeader,
                           String error, String stage) {
        log.error("dtm {}: {}", stage, error);
        if (!properties.isFailOpen()) {
            throw new AmqpRejectAndDontRequeueException(error);
        }
        MessageProperties props = message.getMessageProperties();
        if (statusHeader != null) {
            props.setHeader(statusHeader, "FAILED");
        }
        if (errorHeader != null) {
            props.setHeader(errorHeader, error);
        }
        return message;
    }

    protected void publish(Message message, String routingKey) {
        publisher.publish(MqConstants.EXCHANGE, routingKey, message);
    }

    protected Message withBody(Message original, byte[] body) {
        MessageProperties source = original.getMessageProperties();
        MessageProperties props = new MessageProperties();
        props.getHeaders().putAll(source.getHeaders());
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        props.setDeliveryMode(source.getDeliveryMode());
        props.setPriority(source.getPriority());
        return new Message(body, props);
    }

    protected static byte[] decodeBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(base64);
    }

    protected static String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes == null ? new byte[0] : bytes);
    }

    protected static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
