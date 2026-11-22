package com.iot.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.mq.UplinkProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

/** 兼容 facade 测试：仅验证反序列化后委托给编排服务。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DataIngestionServiceTest {

    @Mock
    private UplinkProcessingService processingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void delegatesToProcessingService() throws Exception {
        UplinkMessage up = new UplinkMessage();
        up.setDeviceId("D1");
        up.setServiceType(24);
        Message msg = new Message(objectMapper.writeValueAsBytes(up), new MessageProperties());

        new DataIngestionService(objectMapper, processingService).onUplink(msg);

        ArgumentCaptor<UplinkMessage> captor = ArgumentCaptor.forClass(UplinkMessage.class);
        verify(processingService).process(captor.capture());
        assertEquals("D1", captor.getValue().getDeviceId());
        assertEquals(24, captor.getValue().getServiceType());
    }

    @Test
    public void badJsonThrowsRuntimeException() {
        Message msg = new Message("not-json".getBytes(), new MessageProperties());
        assertThrows(RuntimeException.class,
                () -> new DataIngestionService(objectMapper, processingService).onUplink(msg));
    }
}
