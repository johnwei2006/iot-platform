package com.iot.dtm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.mq.MessagePublisher;
import com.iot.dtm.profile.DeviceProfileCache;
import com.iot.dtm.protocol.ProtocolRegistry;
import com.iot.dtm.protocol.ProtocolTestSupport;
import com.iot.dtm.protocol.encoder.TransportEncoderManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 上行服务：解码、DeviceProfile 合并、结构化字段、fail-open/fail-close、confirm 失败传播。 */
public class UplinkServiceTest {

    private CodecProperties properties;
    private MessagePublisher publisher;
    private DeviceProfileCache profileCache;
    private UplinkService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws Exception {
        ProtocolRegistry registry = new ProtocolRegistry();
        registry.init();
        TransportEncoderManager manager = ProtocolTestSupport.manager(registry);
        properties = new CodecProperties();
        publisher = mock(MessagePublisher.class);
        profileCache = new DeviceProfileCache(new com.iot.dtm.config.WorkerProperties());
        service = new UplinkService(profileCache, manager, properties, publisher);
    }

    @Test
    public void decodesAndPublishesStructuredUplink() throws Exception {
        Message out = service.process(message(uplinkJson(101, "AQI=", 2)));

        JsonNode body = mapper.readTree(out.getBody());
        assertEquals(body.path("protocolName").asText(), "GATEWAY_REBOOT");
        assertEquals(body.path("decodeStatus").asText(), "OK");
        assertEquals(body.path("transactionType").asText(), "downlink");
        assertEquals(1, body.path("serviceDataDecoded").path("appType").asInt());
        assertEquals(2, body.path("serviceDataDecoded").path("action").asInt());
        assertEquals(body.path("serviceData").asText(), "AQI=");
        assertNotNull(profileCache.get("D1"), "profile should be cached");
        verify(publisher).publish(eq("iot.exchange"), eq("platform.uplink"), any(Message.class));
    }

    @Test
    public void unknownServiceTypeStillForwardsWithUnsupportedStatus() throws Exception {
        Message out = service.process(message(uplinkJson(2, "MTIzNDU2Nzg5MA==", 2)));

        JsonNode body = mapper.readTree(out.getBody());
        assertEquals(body.path("decodeStatus").asText(), "UNSUPPORTED");
        assertFalse(body.has("serviceDataDecoded"));
        assertEquals(body.path("serviceData").asText(), "MTIzNDU2Nzg5MA==");
        verify(publisher).publish(eq("iot.exchange"), eq("platform.uplink"), any(Message.class));
    }

    @Test
    public void invalidJsonFailOpenPublishesOriginalWithHeader() {
        properties.setFailOpen(true);
        Message original = message("not-json");

        Message out = service.process(original);

        assertEquals(new String(out.getBody(), StandardCharsets.UTF_8), "not-json");
        assertEquals(out.getMessageProperties().getHeaders().get("decodeStatus"), "FAILED");
        verify(publisher).publish(eq("iot.exchange"), eq("platform.uplink"), eq(original));
    }

    @Test
    public void invalidJsonFailCloseRejectsWithoutPublishing() {
        properties.setFailOpen(false);
        try {
            service.process(message("not-json"));
            fail("expected AmqpRejectAndDontRequeueException");
        } catch (AmqpRejectAndDontRequeueException expected) {
            assertTrue(expected.getMessage().contains("invalid uplink JSON"));
        }
        verify(publisher, never()).publish(any(String.class), any(String.class), any(Message.class));
    }

    @Test
    public void publisherConfirmFailurePropagates() {
        doThrow(new AmqpException("confirm timeout")).when(publisher)
                .publish(any(String.class), any(String.class), any(Message.class));
        try {
            service.process(message(uplinkJson(101, "AQI=", 2)));
            fail("expected AmqpException");
        } catch (AmqpException expected) {
            assertTrue(expected.getMessage().contains("confirm timeout"));
        }
    }

    private Message message(String json) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }

    private String uplinkJson(int serviceType, String serviceData, int encoding) {
        return "{\"deviceId\":\"D1\",\"nodeId\":\"\",\"eventId\":1,\"serviceType\":" + serviceType
                + ",\"serviceVersion\":1,\"serviceDataEncoding\":" + encoding
                + ",\"serviceDataProtocolVersion\":1,\"serviceEncryption\":0,"
                + "\"serviceData\":\"" + serviceData + "\"}";
    }
}
