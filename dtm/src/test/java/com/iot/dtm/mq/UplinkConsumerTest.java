package com.iot.dtm.mq;

import com.iot.dtm.config.WorkerProperties;
import com.iot.dtm.service.UplinkService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 上行消费者：worker 成功后 ACK，异常 NACK 进 DLQ，超时 requeue。 */
public class UplinkConsumerTest {

    private UplinkService service;
    private ThreadPoolTaskExecutor executor;
    private Channel channel;
    private UplinkConsumer consumer;
    private Message message;

    @BeforeEach
    public void setUp() {
        service = mock(UplinkService.class);
        executor = mock(ThreadPoolTaskExecutor.class);
        channel = mock(Channel.class);
        WorkerProperties properties = new WorkerProperties();
        properties.setAwaitTimeoutMs(50);
        consumer = new UplinkConsumer(service, executor, properties);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(7L);
        message = new Message("{}".getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    public void acksAfterWorkerSucceeds() throws Exception {
        when(executor.submit(any(Callable.class)))
                .thenReturn(CompletableFuture.completedFuture(message));

        consumer.onUplink(message, channel);

        verify(channel).basicAck(7L, false);
    }

    @Test
    public void nacksToDlqWhenWorkerFails() throws Exception {
        CompletableFuture<Message> failed = new CompletableFuture<Message>();
        failed.completeExceptionally(new RuntimeException("boom"));
        when(executor.submit(any(Callable.class))).thenReturn(failed);

        consumer.onUplink(message, channel);

        verify(channel).basicNack(7L, false, false);
    }

    @Test
    public void requeuesWhenWorkerTimesOut() throws Exception {
        CompletableFuture<Message> pending = new CompletableFuture<Message>();
        when(executor.submit(any(Callable.class))).thenReturn(pending);

        consumer.onUplink(message, channel);

        assertTrue(pending.isCancelled(), "timed-out worker should be cancelled");
        verify(channel).basicNack(7L, false, true);
    }
}
