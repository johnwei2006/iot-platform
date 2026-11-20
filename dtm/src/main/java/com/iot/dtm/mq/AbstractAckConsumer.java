package com.iot.dtm.mq;

import com.iot.dtm.config.WorkerProperties;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 手动 ACK 消费者基类：
 * 把报文提交给 worker 线程处理，listener 线程等待结果；成功后由 listener 线程 ACK，
 * 失败 NACK 进死信。Channel 只在 listener 线程使用，规避跨线程 ACK 的线程安全问题。
 */
public abstract class AbstractAckConsumer {

    private static final Logger log = LoggerFactory.getLogger(AbstractAckConsumer.class);

    protected final ThreadPoolTaskExecutor executor;
    protected final WorkerProperties properties;

    protected AbstractAckConsumer(ThreadPoolTaskExecutor executor, WorkerProperties properties) {
        this.executor = executor;
        this.properties = properties;
    }

    protected void execute(Message message, Channel channel, Callable<Message> task, String stage)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Future<Message> future;
        try {
            future = executor.submit(task);
        } catch (RuntimeException e) {
            // 线程池 shutdown 或饱和时提交失败：requeue，避免丢消息
            log.error("dtm {} submit rejected: {}; requeue", stage, e.getMessage(), e);
            nack(channel, deliveryTag, true);
            return;
        }

        try {
            future.get(properties.getAwaitTimeoutMs(), TimeUnit.MILLISECONDS);
            channel.basicAck(deliveryTag, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("dtm {} interrupted while waiting worker; requeue", stage);
            nack(channel, deliveryTag, true);
        } catch (CancellationException e) {
            log.warn("dtm {} worker cancelled; requeue", stage);
            nack(channel, deliveryTag, true);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("dtm {} worker timeout after {}ms; requeue", stage,
                    properties.getAwaitTimeoutMs());
            nack(channel, deliveryTag, true);
        } catch (ExecutionException e) {
            log.error("dtm {} worker failed: {}", stage,
                    e.getCause() == null ? e.getMessage() : e.getCause().getMessage(), e.getCause());
            nack(channel, deliveryTag, false);
        }
    }

    private void nack(Channel channel, long deliveryTag, boolean requeue) {
        try {
            channel.basicNack(deliveryTag, false, requeue);
        } catch (IOException e) {
            log.error("dtm failed to nack delivery {} (requeue={}): {}", deliveryTag, requeue,
                    e.getMessage(), e);
        }
    }
}
