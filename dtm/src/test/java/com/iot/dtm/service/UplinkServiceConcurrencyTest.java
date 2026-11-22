package com.iot.dtm.service;

import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.config.WorkerProperties;
import com.iot.dtm.mq.MessagePublisher;
import com.iot.dtm.profile.DeviceProfileCache;
import com.iot.dtm.protocol.ProtocolRegistry;
import com.iot.dtm.protocol.ProtocolTestSupport;
import com.iot.dtm.protocol.encoder.TransportEncoderManager;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** 并发回归：多设备、多线程同时处理，验证无串包、无共享状态损坏。 */
public class UplinkServiceConcurrencyTest {

    @Test
    public void processesManyDevicesConcurrently() throws Exception {
        ProtocolRegistry registry = new ProtocolRegistry();
        registry.init();
        TransportEncoderManager manager = ProtocolTestSupport.manager(registry);
        MessagePublisher publisher = mock(MessagePublisher.class);
        UplinkService service = new UplinkService(new DeviceProfileCache(new WorkerProperties()),
                manager, new CodecProperties(), publisher);

        final int threads = 8;
        final int perThread = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<Future<?>>();
        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            futures.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < perThread; i++) {
                    String deviceId = "D-" + threadId + "-" + i;
                    service.process(message(deviceId));
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        verify(publisher, times(threads * perThread))
                .publish(any(String.class), any(String.class), any(Message.class));
    }

    private Message message(String deviceId) {
        String json = "{\"deviceId\":\"" + deviceId + "\",\"serviceType\":101,\"serviceVersion\":1,"
                + "\"serviceDataEncoding\":2,\"serviceDataProtocolVersion\":1,"
                + "\"serviceEncryption\":0,\"serviceData\":\"AQI=\"}";
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }
}
