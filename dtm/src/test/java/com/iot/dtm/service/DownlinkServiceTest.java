package com.iot.dtm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.config.WorkerProperties;
import com.iot.dtm.mq.MessagePublisher;
import com.iot.dtm.profile.DeviceProfileCache;
import com.iot.dtm.protocol.ProtocolRegistry;
import com.iot.dtm.protocol.ProtocolTestSupport;
import com.iot.dtm.protocol.encoder.TransportEncoderManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 下行服务：结构化编码、旧 base64 兼容、空载荷空字符串、扩展字段剥离、fail-close。 */
public class DownlinkServiceTest {

    private CodecProperties properties;
    private MessagePublisher publisher;
    private DownlinkService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws Exception {
        ProtocolRegistry registry = new ProtocolRegistry();
        registry.init();
        TransportEncoderManager manager = ProtocolTestSupport.manager(registry);
        properties = new CodecProperties();
        publisher = mock(MessagePublisher.class);
        service = new DownlinkService(new DeviceProfileCache(new WorkerProperties()), manager,
                properties, publisher);
    }

    @Test
    public void encodesStructuredPayloadAndStripsExtensions() throws Exception {
        String json = "{\"deviceId\":\"D1\",\"serviceType\":101,\"serviceVersion\":1,"
                + "\"serviceDataEncoding\":2,\"serviceDataProtocolVersion\":1,\"serviceEncryption\":0,"
                + "\"serviceDataDecoded\":{\"appType\":1,\"action\":9}}";

        Message out = service.process(message(json));
        JsonNode body = mapper.readTree(out.getBody());

        assertArrayEquals(new byte[]{1, 9},
                Base64.getDecoder().decode(body.path("serviceData").asText()));
        assertFalse(body.has("serviceDataDecoded"), "extension fields must be stripped");
        assertFalse(body.has("protocolName"));
        verify(publisher).publish(eq("iot.exchange"), eq("proxy.downlink"), any(Message.class));
    }

    @Test
    public void legacyBase64PayloadPassesThrough() throws Exception {
        String json = "{\"deviceId\":\"D1\",\"serviceType\":101,\"serviceVersion\":1,"
                + "\"serviceDataEncoding\":2,\"serviceDataProtocolVersion\":1,\"serviceEncryption\":0,"
                + "\"serviceData\":\"QUJD\"}";

        Message out = service.process(message(json));
        JsonNode body = mapper.readTree(out.getBody());

        assertEquals(body.path("serviceData").asText(), "QUJD");
        assertFalse(body.has("serviceDataDecoded"));
    }

    @Test
    public void emptyPayloadProtocolUsesEmptyBase64String() throws Exception {
        String json = "{\"deviceId\":\"D1\",\"serviceType\":1,\"serviceVersion\":1,"
                + "\"serviceDataEncoding\":2,\"serviceDataProtocolVersion\":1,\"serviceEncryption\":0}";

        Message out = service.process(message(json));
        JsonNode body = mapper.readTree(out.getBody());

        assertEquals(body.path("serviceData").asText(), "");
    }

    @Test
    public void encodeFailureFailCloseRejectsWithoutPublishing() {
        properties.setFailOpen(false);
        String json = "{\"deviceId\":\"D1\",\"serviceType\":101,\"serviceVersion\":1,"
                + "\"serviceDataEncoding\":2,\"serviceDataProtocolVersion\":1,\"serviceEncryption\":1,"
                + "\"serviceDataDecoded\":{\"appType\":1,\"action\":9}}";
        try {
            service.process(message(json));
            fail("expected AmqpRejectAndDontRequeueException");
        } catch (AmqpRejectAndDontRequeueException expected) {
            assertEquals(true, expected.getMessage().contains("encode serviceDataDecoded failed"));
        }
        verify(publisher, never()).publish(any(String.class), any(String.class), any(Message.class));
    }

    @Test
    public void encodeFailureRejectsEvenWithFailOpen() {
        properties.setFailOpen(true);
        String json = "{\"deviceId\":\"D1\",\"serviceType\":101,\"serviceVersion\":1,"
                + "\"serviceDataEncoding\":2,\"serviceDataProtocolVersion\":1,\"serviceEncryption\":1,"
                + "\"serviceDataDecoded\":{\"appType\":1,\"action\":9}}";
        try {
            service.process(message(json));
            fail("expected AmqpRejectAndDontRequeueException");
        } catch (AmqpRejectAndDontRequeueException expected) {
            assertEquals(true, expected.getMessage().contains("encode serviceDataDecoded failed"));
        }
        verify(publisher, never()).publish(any(String.class), any(String.class), any(Message.class));
    }

    @Test
    public void nonObjectDecodedPayloadIsRejected() {
        String json = "{\"deviceId\":\"D1\",\"serviceType\":101,\"serviceVersion\":1,"
                + "\"serviceDataEncoding\":2,\"serviceDataProtocolVersion\":1,\"serviceEncryption\":0,"
                + "\"serviceDataDecoded\":[1,2,3]}";
        try {
            service.process(message(json));
            fail("expected AmqpRejectAndDontRequeueException");
        } catch (AmqpRejectAndDontRequeueException expected) {
            assertEquals(true, expected.getMessage().contains("must be a JSON object"));
        }
        verify(publisher, never()).publish(any(String.class), any(String.class), any(Message.class));
    }

    private Message message(String json) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(2L);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }
}
