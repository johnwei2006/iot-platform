package com.iot.dtm.mq;

import com.iot.dtm.config.WorkerProperties;
import com.iot.dtm.service.DownlinkService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 下行消费者：与上行一致的 ACK/NACK 语义。 */
public class DownlinkConsumerTest {

    private DownlinkService service;
    private ThreadPoolTaskExecutor executor;
    private Channel channel;
    private DownlinkConsumer consumer;
    private Message message;

    @BeforeEach
    public void setUp() {
        service = mock(DownlinkService.class);
        executor = mock(ThreadPoolTaskExecutor.class);
        channel = mock(Channel.class);
        WorkerProperties properties = new WorkerProperties();
        properties.setAwaitTimeoutMs(50);
        consumer = new DownlinkConsumer(service, executor, properties);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(9L);
        message = new Message("{}".getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    public void acksAfterWorkerSucceeds() throws Exception {
        when(executor.submit(any(Callable.class)))
                .thenReturn(CompletableFuture.completedFuture(message));

        consumer.onDownlink(message, channel);

        verify(channel).basicAck(9L, false);
    }

    @Test
    public void nacksToDlqWhenWorkerFails() throws Exception {
        CompletableFuture<Message> failed = new CompletableFuture<Message>();
        failed.completeExceptionally(new RuntimeException("boom"));
        when(executor.submit(any(Callable.class))).thenReturn(failed);

        consumer.onDownlink(message, channel);

        verify(channel).basicNack(9L, false, false);
    }
}
