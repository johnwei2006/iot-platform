package com.iot.platform.service.downlink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.platform.mq.DownlinkMessage;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DownlinkHandlersTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void validationAcceptsNonBlankDeviceId() {
        DownlinkMessage dm = message("D1");
        new DownlinkValidationHandler().handle(dm);
        assertEquals("D1", dm.getDeviceId());
    }

    @Test
    public void validationRejectsBlankDeviceId() {
        try {
            new DownlinkValidationHandler().handle(message("  "));
            fail("blank deviceId should be rejected");
        } catch (IllegalArgumentException e) {
            assertEquals("deviceId is required for downlink", e.getMessage());
        }
    }

    @Test
    public void normalizationSetsEmptyPayloadWhenBothPayloadsMissing() {
        DownlinkMessage dm = message("D2");

        new DownlinkNormalizationHandler().handle(dm);

        assertEquals("", dm.getServiceData());
    }

    @Test
    public void normalizationKeepsBase64Payload() {
        DownlinkMessage dm = message("D3");
        dm.setServiceData("AQI=");

        new DownlinkNormalizationHandler().handle(dm);

        assertEquals("AQI=", dm.getServiceData());
    }

    @Test
    public void normalizationKeepsStructuredPayload() {
        DownlinkMessage dm = message("D4");
        ObjectNode decoded = mapper.createObjectNode();
        decoded.put("action", 9);
        dm.setServiceDataDecoded(decoded);

        new DownlinkNormalizationHandler().handle(dm);

        assertEquals(null, dm.getServiceData());
        assertEquals(9, dm.getServiceDataDecoded().path("action").asInt());
    }

    @Test
    public void publishSerializesAndSendsToConfiguredExchange() throws Exception {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        DownlinkMessage dm = message("D5");
        dm.setServiceType(101);

        new DownlinkPublishHandler(rabbitTemplate, mapper, "iot.exchange", "platform.downlink")
                .handle(dm);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("iot.exchange"), eq("platform.downlink"), captor.capture());
        com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(captor.getValue().getBody());
        assertEquals("D5", json.path("deviceId").asText());
        assertEquals(101, json.path("serviceType").asInt());
    }

    @Test
    public void publishWrapsException() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        doThrow(new RuntimeException("broker down"))
                .when(rabbitTemplate).send(any(String.class), any(String.class), any(Message.class));

        try {
            new DownlinkPublishHandler(rabbitTemplate, mapper, "iot.exchange", "platform.downlink")
                    .handle(message("D6"));
            fail("publish failure should propagate");
        } catch (RuntimeException e) {
            assertEquals("broker down", e.getCause().getMessage());
        }
    }

    @Test
    public void handlersAreNamedAndOrdered() {
        assertEquals("downlinkValidation", new DownlinkValidationHandler().name());
        assertEquals("downlinkNormalization", new DownlinkNormalizationHandler().name());
        assertEquals("downlinkPublish",
                new DownlinkPublishHandler(mock(RabbitTemplate.class), mapper, "e", "r").name());
    }

    private DownlinkMessage message(String deviceId) {
        DownlinkMessage dm = new DownlinkMessage();
        dm.setDeviceId(deviceId);
        return dm;
    }
}
