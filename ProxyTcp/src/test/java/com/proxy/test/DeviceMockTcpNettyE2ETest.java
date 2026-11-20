package com.proxy.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.proxy.model.Message;
import com.proxy.netty.ChannelRegistry;
import com.proxy.netty.NettyTcpServer;
import com.proxy.test.protocol.MockDecodeResult;
import com.proxy.test.protocol.MockProtocolCodec;
import com.proxy.test.protocol.MockServiceCase;

/**
 * DeviceMockTCP × ProxyTcp Netty 接入层端到端测试。
 *
 * <p>启动真实的 NettyTcpServer（UplinkFrameDecoder + UplinkChannelHandler +
 * NettyDownlinkSender），DeviceMockTCP 通过真实 TCP 逐条发送 37 类业务上行；
 * 测试侧从 receiveQueue 取 payload 校验，再经 sendQueue 回写同类型下行，
 * DeviceMockTCP 接收并解码业务字段。
 *
 * <p>不依赖 broker / DB；不修改生产代码。
 */
public class DeviceMockTcpNettyE2ETest {

    @Configuration
    @ComponentScan(basePackages = "com.proxy.netty")
    static class NettyConfig {
        @Bean
        public LinkedBlockingQueue<Message> receiveQueue() {
            return new LinkedBlockingQueue<Message>();
        }

        @Bean
        public LinkedBlockingQueue<Message> sendQueue() {
            return new LinkedBlockingQueue<Message>(64);
        }
    }

    @Test(timeout = 90000)
    public void sendsAndReceivesAll37ServiceTypes() throws Exception {
        System.setProperty("tcpport", "0");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(NettyConfig.class);
        DeviceMockTCP device = null;
        try {
            NettyTcpServer server = ctx.getBean(NettyTcpServer.class);
            ChannelRegistry registry = ctx.getBean(ChannelRegistry.class);
            @SuppressWarnings("unchecked")
            LinkedBlockingQueue<Message> receiveQueue =
                    ctx.getBean("receiveQueue", LinkedBlockingQueue.class);
            @SuppressWarnings("unchecked")
            LinkedBlockingQueue<Message> sendQueue =
                    ctx.getBean("sendQueue", LinkedBlockingQueue.class);

            startServer(server);
            int port = awaitBoundPort(server);
            assertTrue("Netty server should bind an ephemeral port", port > 0);

            MockProtocolCodec codec = new MockProtocolCodec();
            List<MockServiceCase> cases = codec.allCases();
            assertEquals(37, cases.size());

            String deviceId = "30410007102";
            device = new DeviceMockTCP(codec);
            device.setDeviceId(deviceId);
            device.connect("127.0.0.1", port);

            int index = 0;
            for (MockServiceCase testCase : cases) {
                index++;
                int serviceType = testCase.getServiceType();
                byte[] serviceData;
                if (testCase.isUnsupported()) {
                    serviceData = new byte[] {1, 2, 3, 4, 5};
                    device.sendRawService(serviceType, serviceData);
                } else {
                    serviceData = codec.encode(serviceType, testCase.getSample());
                    device.sendService(serviceType, testCase.getSample());
                }

                Message uplink = receiveQueue.poll(5, TimeUnit.SECONDS);
                assertNotNull("uplink not received for " + testCase, uplink);
                MockTransportMessage parsed = MockFrameCodec.decodePayload(
                        uplink.getPayload(), codec);
                assertEquals("serviceType mismatch for " + testCase,
                        serviceType, parsed.getServiceType());
                assertEquals("deviceId mismatch for " + testCase,
                        deviceId, parsed.getDeviceId());
                assertArrayEquals("serviceData mismatch for " + testCase,
                        serviceData, parsed.getServiceData());

                if (index == 1) {
                    registry.bindDevice(deviceId, uplink.getIp(), uplink.getPort());
                    assertTrue("device should bind to Netty channel",
                            registry.getOnlineDeviceIds().contains(deviceId));
                }

                int eventId = 7000 + index;
                int messageId = 8000 + index;
                byte[] downlinkPayload = MockFrameCodec.buildPayload(
                        deviceId, serviceType, serviceData, eventId, messageId);
                Message downlink = new Message();
                downlink.setDeviceId(deviceId);
                downlink.setPayload(downlinkPayload);
                assertTrue("sendQueue should accept downlink", sendQueue.offer(downlink));

                MockTransportMessage received = device.awaitMessage(5000);
                assertNotNull("downlink not received for " + testCase, received);
                assertEquals("downlink serviceType mismatch for " + testCase,
                        serviceType, received.getServiceType());
                assertEquals("downlink eventId mismatch for " + testCase,
                        eventId, received.getEventId());
                assertEquals("downlink messageId mismatch for " + testCase,
                        messageId, received.getMessageId());
                assertNotNull("business result missing for " + testCase,
                        received.getBusiness());

                if (testCase.isUnsupported()) {
                    assertEquals("unsupported protocol should report UNSUPPORTED",
                            MockDecodeResult.Status.UNSUPPORTED,
                            received.getBusiness().getStatus());
                } else {
                    assertTrue("downlink business decode failed for " + testCase
                                    + ": " + received.getBusiness(),
                            received.getBusiness().isSuccess());
                    assertArrayEquals("downlink re-encode mismatch for " + testCase,
                            serviceData,
                            codec.encode(serviceType, received.getBusiness().getBusiness()));
                }
            }

            assertEquals("all 37 service types should be received",
                    37L, device.getReceiveCount());
        } finally {
            if (device != null) {
                device.close();
            }
            ctx.close();
            System.clearProperty("tcpport");
        }
    }

    private static void startServer(final NettyTcpServer server) throws Exception {
        final Throwable[] error = {null};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server.start();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    error[0] = t;
                }
            }
        }, "netty-e2e-server");
        thread.setDaemon(true);
        thread.start();
        for (int i = 0; i < 100; i++) {
            if (error[0] != null) {
                throw new IllegalStateException("Netty server failed to start", error[0]);
            }
            if (server.getBoundPort() > 0) {
                return;
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("Netty server did not bind within 5s");
    }

    private static int awaitBoundPort(NettyTcpServer server) {
        return server.getBoundPort();
    }
}
