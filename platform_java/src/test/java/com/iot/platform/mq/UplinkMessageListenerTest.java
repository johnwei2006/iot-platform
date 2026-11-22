package com.iot.platform.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UplinkMessageListenerTest {

    @Mock
    private UplinkProcessingService processingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void deserializesAndDelegates() throws Exception {
        UplinkMessage up = new UplinkMessage();
        up.setDeviceId("D1");
        up.setServiceType(24);
        Message msg = new Message(objectMapper.writeValueAsBytes(up), new MessageProperties());

        new UplinkMessageListener(objectMapper, processingService).onUplink(msg);

        verify(processingService).process(ArgumentMatchers.argThat(
                (UplinkMessage m) -> "D1".equals(m.getDeviceId()) && m.getServiceType() == 24));
    }

    @Test
    public void processingFailurePropagatesAsRuntimeException() throws Exception {
        UplinkMessage up = new UplinkMessage();
        up.setDeviceId("D2");
        Message msg = new Message(objectMapper.writeValueAsBytes(up), new MessageProperties());
        doThrow(new RuntimeException("db down")).when(processingService).process(any(UplinkMessage.class));

        try {
            new UplinkMessageListener(objectMapper, processingService).onUplink(msg);
            fail("processing failure should propagate");
        } catch (RuntimeException e) {
            assertEquals("db down", e.getCause().getMessage());
        }
    }

    @Test
    public void badJsonIsWrappedAsRuntimeException() {
        Message msg = new Message("not-json".getBytes(), new MessageProperties());
        assertThrows(RuntimeException.class,
                () -> new UplinkMessageListener(objectMapper, processingService).onUplink(msg));
    }
}
