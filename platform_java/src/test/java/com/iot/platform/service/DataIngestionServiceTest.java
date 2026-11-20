package com.iot.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.mq.UplinkProcessingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

/** 兼容 facade 测试：仅验证反序列化后委托给编排服务。 */
@RunWith(MockitoJUnitRunner.class)
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

    @Test(expected = RuntimeException.class)
    public void badJsonThrowsRuntimeException() {
        Message msg = new Message("not-json".getBytes(), new MessageProperties());
        new DataIngestionService(objectMapper, processingService).onUplink(msg);
    }
}
