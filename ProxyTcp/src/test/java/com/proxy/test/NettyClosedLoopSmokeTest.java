package com.proxy.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.Message;
import com.proxy.netty.ChannelRegistry;
import com.proxy.netty.NettyTcpServer;
import com.proxy.utils.ByteUtil;

/**
 * Netty 上行+下行闭环冒烟（自包含，不依赖 broker / DB）。
 *
 * <p>仅启动 Netty 接入层相关 Bean（NettyTcpServer / UplinkChannelHandler / ChannelRegistry /
 * NettyDownlinkSender + receiveQueue/sendQueue），不走经典阻塞 I/O，也不加载 MQ/DB。
 *
 * <p>流程：模拟设备连 50001 发上行帧 -> Netty 解码并建立 Channel -> 绑定 deviceId（真实链路由
 * TransportUplinkProcessor 完成，这里等价调用 bindDevice）-> 平台下行经 sendQueue ->
 * NettyDownlinkSender 按 deviceId 取 Channel 写回 -> 设备收到 FF EE + 小端长度 + payload 帧。
 *
 * <p>类名以 Test 结尾，可被 `mvn test` 默认执行；也可显式 `mvn test -Dtest=NettyClosedLoopSmokeTest`。
 */
public class NettyClosedLoopSmokeTest {

    @Configuration
    @ComponentScan(basePackages = "com.proxy.netty")
    static class SmokeConfig {
        @Bean
        public LinkedBlockingQueue<Message> receiveQueue() {
            return new LinkedBlockingQueue<>();
        }

        @Bean
        public LinkedBlockingQueue<Message> sendQueue() {
            return new LinkedBlockingQueue<>(50);
        }
    }

    @Test
    public void closedLoopUplinkDownlink() throws Exception {
        // 使用临时端口避免本地 50001 被占用导致 bind 失败（默认 50001 仅在未显式设置时生效）
        System.setProperty("tcpport", "0");
        // 网关认证服务用 Mockito 桩：refresh 前手动注册单例，绕开 Spring 对 mock 的字段注入
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getBeanFactory().registerSingleton("gatewayAuthService",
                org.mockito.Mockito.mock(com.proxy.auth.GatewayAuthService.class));
        ctx.register(SmokeConfig.class);
        ctx.refresh();
        try {
            NettyTcpServer server = ctx.getBean(NettyTcpServer.class);
            ChannelRegistry registry = ctx.getBean(ChannelRegistry.class);
            @SuppressWarnings("unchecked")
            LinkedBlockingQueue<Message> sendQueue = ctx.getBean("sendQueue", LinkedBlockingQueue.class);

            // NettyTcpServer.start() 末尾 closeFuture().sync() 会阻塞，放守护线程
            final Throwable[] serverError = {null};
            Thread serverThread = new Thread(() -> {
                try {
                    server.start();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    serverError[0] = t;
                    System.err.println("Netty server start failed: " + t);
                }
            }, "netty-server");
            serverThread.setDaemon(true);
            serverThread.start();
            int boundPort = -1;
            for (int i = 0; i < 50; i++) {
                if (serverError[0] != null) {
                    throw new IllegalStateException("Netty server failed to start", serverError[0]);
                }
                boundPort = server.getBoundPort();
                if (boundPort > 0) {
                    break;
                }
                Thread.sleep(100);
            }
            System.out.println("DEBUG alive=" + serverThread.isAlive()
                    + " boundPort=" + boundPort + " err=" + serverError[0]);
            assertTrue(boundPort > 0, "Netty 应已绑定到临时端口");
            System.out.println("Netty TCP server bound on " + boundPort);

            String deviceId = "30410007102";
            try (Socket sock = new Socket("127.0.0.1", boundPort)) {
                int localPort = sock.getLocalPort();
                OutputStream out = sock.getOutputStream();

                // 上行：构造合法帧 FF EE + 小端长度 + payload（复用 DeviceMockTCP 编码逻辑）
                TransportDataEncoder encoder = new TransportDataEncoder();
                byte[] serviceData = "1234567890".getBytes();
                DownlinkTransportMessage dmsg = encoder.mockDownlinkTransportMessage(deviceId, 2, serviceData);
                byte[] uplinkPayload = encoder.createUplinkMessage(dmsg);
                out.write(wrapFrame(uplinkPayload));
                out.flush();
                Thread.sleep(300); // 等待 Netty 解码 + 注册 Channel

                // 等价 TransportUplinkProcessor 解码出 deviceId 后的绑定（真实链路由处理器完成）
                registry.bindDevice(deviceId, "127.0.0.1", localPort);
                assertTrue(registry.getOnlineDeviceIds().contains(deviceId), "deviceId 应已绑定到 Channel");

                // 下行：平台经 sendQueue -> NettyDownlinkSender -> Channel 写回
                byte[] downPayload = "HELLO-DOWNLINK".getBytes();
                Message dm = new Message();
                dm.setDeviceId(deviceId);
                dm.setPayload(downPayload);
                sendQueue.offer(dm);

                // 设备侧读取下行帧
                InputStream in = sock.getInputStream();
                sock.setSoTimeout(5000);
                byte[] received = readExact(in, 4 + downPayload.length);
                assertNotNull(received, "设备应在超时内收到下行帧");
                assertTrue(received[0] == (byte) 0xFF && received[1] == (byte) 0xEE, "下行帧头应为 FF EE");
                byte[] tail = Arrays.copyOfRange(received, 4, received.length);
                assertArrayEquals(downPayload, tail, "下行 payload 应与发送一致");
                System.out.println("CLOSED-LOOP SMOKE PASS: device " + deviceId
                        + " received downlink (" + new String(tail) + ")");
            }
        } finally {
            ctx.close(); // 触发 @PreDestroy 优雅关闭 Netty
        }
    }

    private static byte[] wrapFrame(byte[] payload) {
        byte[] frame = new byte[payload.length + 4];
        frame[0] = (byte) 0xFF;
        frame[1] = (byte) 0xEE;
        byte[] len = ByteUtil.short2BigByte2Array(payload.length);
        frame[2] = len[0];
        frame[3] = len[1];
        System.arraycopy(payload, 0, frame, 4, payload.length);
        return frame;
    }

    private static byte[] readExact(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r == -1) {
                break;
            }
            off += r;
        }
        return off == n ? buf : null;
    }
}
