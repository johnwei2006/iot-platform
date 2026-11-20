package com.iot.dtm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.mq.MessagePublisher;
import com.iot.dtm.profile.DeviceProfileCache;
import com.iot.dtm.protocol.ProtocolRegistry;
import com.iot.dtm.protocol.ProtocolTestSupport;
import com.iot.dtm.protocol.encoder.TransportEncoderManager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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

    @Before
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
        assertEquals("GATEWAY_REBOOT", body.path("protocolName").asText());
        assertEquals("OK", body.path("decodeStatus").asText());
        assertEquals("downlink", body.path("transactionType").asText());
        assertEquals(1, body.path("serviceDataDecoded").path("appType").asInt());
        assertEquals(2, body.path("serviceDataDecoded").path("action").asInt());
        assertEquals("AQI=", body.path("serviceData").asText());
        assertNotNull("profile should be cached", profileCache.get("D1"));
        verify(publisher).publish(eq("iot.exchange"), eq("platform.uplink"), any(Message.class));
    }

    @Test
    public void unknownServiceTypeStillForwardsWithUnsupportedStatus() throws Exception {
        Message out = service.process(message(uplinkJson(2, "MTIzNDU2Nzg5MA==", 2)));

        JsonNode body = mapper.readTree(out.getBody());
        assertEquals("UNSUPPORTED", body.path("decodeStatus").asText());
        assertFalse(body.has("serviceDataDecoded"));
        assertEquals("MTIzNDU2Nzg5MA==", body.path("serviceData").asText());
        verify(publisher).publish(eq("iot.exchange"), eq("platform.uplink"), any(Message.class));
    }

    @Test
    public void invalidJsonFailOpenPublishesOriginalWithHeader() {
        properties.setFailOpen(true);
        Message original = message("not-json");

        Message out = service.process(original);

        assertEquals("not-json", new String(out.getBody(), StandardCharsets.UTF_8));
        assertEquals("FAILED", out.getMessageProperties().getHeaders().get("decodeStatus"));
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
