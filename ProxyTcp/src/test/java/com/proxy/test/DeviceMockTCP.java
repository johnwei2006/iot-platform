package com.proxy.test;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxy.test.protocol.MockDecodeResult;
import com.proxy.test.protocol.MockProtocolCodec;
import com.proxy.test.protocol.MockProtocolCodecTest;
import com.proxy.test.protocol.MockServiceCase;

/**
 * 模拟设备 TCP 客户端。
 *
 * <p>既可以作为测试 API 使用（sendService / sendAllServices / awaitMessage），
 * 也可以直接运行 main：
 *
 * <pre>
 * DeviceMockTCP                 无参数 / --test：运行内置完整测试套件
 * DeviceMockTCP --live          连接本机 50001，发送全部 37 类业务并等待下行
 * DeviceMockTCP [host] [port] [--all] [--type=101] [--json={...}]
 *               [--listen=30] [--device-id=30410007102]
 * </pre>
 *
 * <p>发送：按 protocol-schemas.json 编码业务字段，经 TransportDataEncoder 组装
 * 4 字节传输头 + 41 字节 transport + serviceData + CRC，再包 FF EE 帧。
 * 接收：独立线程按帧读取，校验 CRC，解析 transport 与业务字段并打印。
 */
public class DeviceMockTCP implements Closeable {

    private static final int DEFAULT_PORT = 50001;
    private static final String DEFAULT_DEVICE_ID = "30410007102";
    private static final int MAX_RECEIVE_QUEUE = 256;
    private static final int RECEIVE_TIMEOUT_MS = 5000;
    private static final int DEFAULT_LISTEN_SECONDS = 20;

    private static final Class<?>[] BUILTIN_TEST_CLASSES = {
            MockProtocolCodecTest.class,
            ProtocolSchemaSyncTest.class,
            DeviceMockTcpNettyE2ETest.class
    };

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MockProtocolCodec codec;
    private final BlockingQueue<MockTransportMessage> received =
            new LinkedBlockingQueue<MockTransportMessage>(MAX_RECEIVE_QUEUE);
    private final Object sendLock = new Object();
    private final AtomicInteger eventSequence = new AtomicInteger(1000);
    private final AtomicInteger messageSequence = new AtomicInteger(1);
    private final AtomicLong receiveCount = new AtomicLong();

    private volatile Socket socket;
    private volatile OutputStream out;
    private volatile InputStream in;
    private volatile boolean connected;
    private volatile Thread receiveThread;
    private volatile String deviceId = DEFAULT_DEVICE_ID;

    public DeviceMockTCP() {
        this(new MockProtocolCodec());
    }

    public DeviceMockTCP(MockProtocolCodec codec) {
        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }
        this.codec = codec;
    }

    public MockProtocolCodec getCodec() {
        return codec;
    }

    public void setDeviceId(String deviceId) {
        if (deviceId != null && deviceId.length() > 0) {
            this.deviceId = deviceId;
        }
    }

    public String getDeviceId() {
        return deviceId;
    }

    /**
     * 建立 TCP 连接并启动接收线程。
     */
    public void connect(String host, int port) throws IOException {
        if (connected) {
            throw new IOException("already connected");
        }
        Socket newSocket = new Socket();
        newSocket.setTcpNoDelay(true);
        newSocket.connect(new InetSocketAddress(host, port), RECEIVE_TIMEOUT_MS);
        this.socket = newSocket;
        this.in = newSocket.getInputStream();
        this.out = newSocket.getOutputStream();
        this.connected = true;
        startReceiveThread();
        System.out.println("[DeviceMockTCP] connected to " + host + ":" + port
                + ", localPort=" + newSocket.getLocalPort());
    }

    public boolean isConnected() {
        return connected;
    }

    public int getLocalPort() {
        Socket s = socket;
        return s == null ? -1 : s.getLocalPort();
    }

    private void startReceiveThread() {
        receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                receiveLoop();
            }
        }, "device-mock-tcp-receiver");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    private void receiveLoop() {
        while (connected) {
            try {
                byte[] payload = MockFrameCodec.readFrame(in);
                if (payload == null) {
                    break;
                }
                MockTransportMessage message = MockFrameCodec.decodePayload(payload, codec);
                receiveCount.incrementAndGet();
                if (!received.offer(message)) {
                    System.err.println("[DeviceMockTCP] receive queue full, drop serviceType="
                            + message.getServiceType());
                }
                printBusiness(message);
            } catch (IOException e) {
                if (!connected) {
                    break;
                }
                System.err.println("[DeviceMockTCP] receive/frame error: " + e.getMessage());
                // 帧头/长度错误后流已无法保证对齐，退出接收线程；业务解码错误已在
                // decodePayload 内转成 MockDecodeResult，不会走到这里。
                break;
            } catch (Exception e) {
                System.err.println("[DeviceMockTCP] decode error: " + e.getMessage());
            }
        }
        if (connected) {
            System.err.println("[DeviceMockTCP] receive loop stopped");
        }
    }

    /**
     * 按 schema 编码并发送一条结构化业务报文（二进制 encoding=2）。
     */
    public MockTransportMessage sendService(int serviceType, JsonNode decoded) throws Exception {
        byte[] serviceData = codec.encode(serviceType, decoded);
        return sendRawService(serviceType, serviceData, MockProtocolCodec.ENCODING_BINARY);
    }

    /**
     * 以 JSON encoding=1 发送，便于联调时验证网关的 JSON 分支。
     */
    public MockTransportMessage sendJsonService(int serviceType, JsonNode decoded) throws Exception {
        byte[] serviceData = MAPPER.writeValueAsBytes(decoded);
        return sendRawService(serviceType, serviceData, MockProtocolCodec.ENCODING_JSON);
    }

    public MockTransportMessage sendRawService(int serviceType, byte[] serviceData) throws Exception {
        return sendRawService(serviceType, serviceData, MockProtocolCodec.ENCODING_BINARY);
    }

    /**
     * 直接发送原始 serviceData（0x60 等无 schema 协议、故障注入场景）。
     */
    public MockTransportMessage sendRawService(int serviceType, byte[] serviceData, int encoding)
            throws Exception {
        if (!connected) {
            throw new IOException("not connected");
        }
        byte[] data = serviceData == null ? new byte[0] : serviceData;
        int eventId = eventSequence.incrementAndGet();
        int messageId = messageSequence.incrementAndGet();
        byte[] payload = MockFrameCodec.buildPayload(deviceId, serviceType, data,
                encoding, eventId, messageId);
        writeFrame(payload);
        MockTransportMessage sent = sentMessage(serviceType, data, encoding, eventId, messageId, payload);
        System.out.println("[DeviceMockTCP] sent " + codec.hex(serviceType) + " "
                + codec.name(serviceType) + " serviceData=" + data.length
                + " bytes, messageId=" + messageId + ", eventId=" + eventId);
        return sent;
    }

    /**
     * 遍历 schema 中全部协议发送一遍。0x60 没有字段定义，使用原始字节。
     */
    public int sendAllServices() throws Exception {
        int sent = 0;
        for (MockServiceCase testCase : codec.allCases()) {
            if (testCase.isUnsupported()) {
                sendRawService(testCase.getServiceType(), rawSample(), MockProtocolCodec.ENCODING_BINARY);
            } else {
                sendService(testCase.getServiceType(), testCase.getSample());
            }
            sent++;
        }
        return sent;
    }

    public int sendAllServices(List<Integer> serviceTypes) throws Exception {
        int sent = 0;
        for (Integer type : serviceTypes) {
            if (type == null) {
                continue;
            }
            if (codec.protocol(type) != null && codec.protocol(type).path("unsupported").asBoolean()) {
                sendRawService(type, rawSample(), MockProtocolCodec.ENCODING_BINARY);
            } else {
                sendService(type, codec.sample(type));
            }
            sent++;
        }
        return sent;
    }

    private static byte[] rawSample() {
        return new byte[] {0x01, 0x02, 0x03, 0x04, 0x05};
    }

    private MockTransportMessage sentMessage(int serviceType, byte[] serviceData, int encoding,
                                             int eventId, int messageId, byte[] payload) {
        MockTransportMessage message = new MockTransportMessage();
        message.setTransportDataEncoding(2);
        message.setTransportVersion(1);
        message.setTransportDataLength(MockFrameCodec.TRANSPORT_PART_LENGTH + serviceData.length);
        message.setDeviceId(deviceId);
        message.setNodeId("");
        message.setMessageCreationTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        message.setMessageId(messageId);
        message.setServiceType(serviceType);
        message.setServiceVersion(1);
        message.setServiceDataEncoding(encoding);
        message.setServiceDataProtocolVersion(1);
        message.setServiceDataEncryption(0);
        message.setEventId(eventId);
        message.setServiceData(serviceData);
        message.setFrameLength(payload.length + MockFrameCodec.FRAME_HEADER_SIZE);
        message.setBusiness(codec.decode(serviceType, encoding, 1, 0, serviceData));
        return message;
    }

    /**
     * 兼容旧用法：直接发送已组好的字节（例如手工构造的完整 FF EE 帧）。
     */
    public void sendBytes(byte[] data) throws IOException {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        writeFrameBytes(data);
        System.out.println("[DeviceMockTCP] sent raw bytes=" + data.length);
    }

    private void writeFrame(byte[] payload) throws IOException {
        writeFrameBytes(MockFrameCodec.wrapFrame(payload));
    }

    private void writeFrameBytes(byte[] bytes) throws IOException {
        synchronized (sendLock) {
            OutputStream stream = out;
            if (stream == null || !connected) {
                throw new IOException("not connected");
            }
            stream.write(bytes);
            stream.flush();
        }
    }

    /**
     * 阻塞等待一条下行；超时返回 null。
     */
    public MockTransportMessage awaitMessage(long timeoutMillis) throws InterruptedException {
        return received.poll(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public List<MockTransportMessage> drainMessages() {
        List<MockTransportMessage> list = new ArrayList<MockTransportMessage>();
        received.drainTo(list);
        return list;
    }

    public long getReceiveCount() {
        return receiveCount.get();
    }

    public BlockingQueue<MockTransportMessage> getReceiveQueue() {
        return received;
    }

    private void printBusiness(MockTransportMessage msg) {
        MockDecodeResult result = msg.getBusiness();
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append("[DOWNLINK] ").append(codec.hex(msg.getServiceType()))
                .append(" ").append(codec.name(msg.getServiceType()))
                .append(" direction=").append(codec.direction(msg.getServiceType()))
                .append(System.lineSeparator());
        sb.append("  deviceId=").append(msg.getDeviceId())
                .append(", nodeId=").append(msg.getNodeId())
                .append(", messageId=").append(msg.getMessageId())
                .append(", eventId=").append(msg.getEventId()).append(System.lineSeparator());
        sb.append("  time=").append(msg.getMessageCreationTime())
                .append(", serviceVersion=").append(msg.getServiceVersion())
                .append(", transportEncoding=").append(msg.getTransportDataEncoding())
                .append(", transportVersion=").append(msg.getTransportVersion())
                .append(", frameLength=").append(msg.getFrameLength()).append(System.lineSeparator());
        sb.append("  serviceDataEncoding=").append(msg.getServiceDataEncoding())
                .append(", protocolVersion=").append(msg.getServiceDataProtocolVersion())
                .append(", encryption=").append(msg.getServiceDataEncryption())
                .append(", serviceDataLength=").append(msg.getServiceData().length).append(System.lineSeparator());
        sb.append("  serviceData(hex)=").append(abbreviate(toHex(msg.getServiceData()), 512))
                .append(System.lineSeparator());
        if (result == null) {
            sb.append("  business=null");
        } else {
            sb.append("  business=").append(result.getStatus());
            if (result.getError() != null) {
                sb.append(" (").append(result.getError()).append(")");
            }
            if (result.getBusiness() != null) {
                try {
                    sb.append(System.lineSeparator())
                            .append(MAPPER.writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(result.getBusiness()));
                } catch (Exception e) {
                    sb.append(" [").append(result.getBusiness().toString()).append("]");
                }
            }
        }
        System.out.println(sb.toString());
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "null";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...(" + text.length() + " chars)";
    }

    private static String toHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        char[] hex = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = hex[v >>> 4];
            out[i * 2 + 1] = hex[v & 0x0F];
        }
        return new String(out);
    }

    @Override
    public void close() {
        boolean wasConnected = connected;
        connected = false;
        Socket s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
        Thread t = receiveThread;
        if (t != null && t != Thread.currentThread()) {
            t.interrupt();
            try {
                t.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (wasConnected) {
            System.out.println("[DeviceMockTCP] closed");
        }
    }

    // ===================== CLI =====================

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = DEFAULT_PORT;
        boolean all = false;
        boolean testMode = false;
        boolean live = false;
        boolean listenSpecified = false;
        int listenSeconds = DEFAULT_LISTEN_SECONDS;
        String deviceId = DEFAULT_DEVICE_ID;
        String json = null;
        List<Integer> types = new ArrayList<Integer>();
        List<String> positional = new ArrayList<String>();

        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--help".equals(arg) || "-h".equals(arg)) {
                    printUsage();
                    return;
                }
                if ("--test".equals(arg)) {
                    testMode = true;
                } else if ("--live".equals(arg)) {
                    live = true;
                } else if ("--all".equals(arg)) {
                    all = true;
                } else if (arg.startsWith("--type=")) {
                    types.add(Integer.decode(arg.substring("--type=".length())));
                } else if ("--type".equals(arg) && i + 1 < args.length) {
                    types.add(Integer.decode(args[++i]));
                } else if (arg.startsWith("--json=")) {
                    json = arg.substring("--json=".length());
                } else if ("--json".equals(arg) && i + 1 < args.length) {
                    json = args[++i];
                } else if (arg.startsWith("--listen=")) {
                    listenSeconds = Integer.parseInt(arg.substring("--listen=".length()));
                    listenSpecified = true;
                } else if ("--listen".equals(arg) && i + 1 < args.length) {
                    listenSeconds = Integer.parseInt(args[++i]);
                    listenSpecified = true;
                } else if (arg.startsWith("--device-id=")) {
                    deviceId = arg.substring("--device-id=".length());
                } else if ("--device-id".equals(arg) && i + 1 < args.length) {
                    deviceId = args[++i];
                } else if (arg.startsWith("--")) {
                    throw new IllegalArgumentException("unknown option: " + arg);
                } else {
                    positional.add(arg);
                }
            }
            if (positional.size() > 0) {
                host = positional.get(0);
            }
            if (positional.size() > 1) {
                port = Integer.parseInt(positional.get(1));
            }
        } catch (RuntimeException e) {
            System.err.println("argument error: " + e.getMessage());
            printUsage();
            return;
        }

        // 无参数（或 --test）默认执行完整测试套件，避免只发一条示例报文就退出。
        boolean explicitLive = live || !positional.isEmpty() || all
                || !types.isEmpty() || json != null || listenSpecified;
        if (testMode || !explicitLive) {
            runBuiltInTests();
            return;
        }
        if (live && types.isEmpty()) {
            all = true;
        }

        DeviceMockTCP client = new DeviceMockTCP();
        client.setDeviceId(deviceId);
        int sent = 0;
        try {
            client.connect(host, port);
            if (all) {
                sent = client.sendAllServices();
                System.out.println("[DeviceMockTCP] sent all " + sent + " service types");
            } else if (!types.isEmpty()) {
                JsonNode custom = null;
                if (json != null) {
                    custom = MAPPER.readTree(json);
                }
                for (Integer type : types) {
                    if (custom != null && custom.isObject()) {
                        client.sendService(type, custom);
                    } else if (codecUnsupported(type)) {
                        client.sendRawService(type, rawSample());
                    } else {
                        client.sendService(type, client.getCodec().sample(type));
                    }
                    sent++;
                }
            } else {
                client.sendService(101, client.getCodec().sample(101));
                sent = 1;
            }

            long deadline = System.currentTimeMillis() + Math.max(listenSeconds, 0) * 1000L;
            List<MockTransportMessage> receivedMessages = new ArrayList<MockTransportMessage>();
            while (System.currentTimeMillis() < deadline) {
                long remain = deadline - System.currentTimeMillis();
                MockTransportMessage message = client.awaitMessage(Math.max(1L, Math.min(remain, 500L)));
                if (message != null) {
                    receivedMessages.add(message);
                }
            }
            printLiveSummary(sent, receivedMessages);
        } catch (Exception e) {
            System.err.println("[DeviceMockTCP] failed: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            client.close();
        }
    }

    /**
     * 内置完整测试套件：37 协议编解码 + schema 同步 + 真实 Netty 37 类上下行闭环。
     * 不依赖外部 ProxyTcp / RabbitMQ / DB。
     */
    private static void runBuiltInTests() {
        System.out.println("[DeviceMockTCP] no live args: running built-in full test suite ("
                + BUILTIN_TEST_CLASSES.length + " test classes)");
        JUnitCore core = new JUnitCore();
        core.addListener(new RunListener() {
            @Override
            public void testFinished(Description description) {
                System.out.println("[TEST PASS] " + description.getClassName()
                        + "." + description.getMethodName());
            }

            @Override
            public void testFailure(Failure failure) {
                System.err.println("[TEST FAIL] " + failure.getTestHeader()
                        + ": " + failure.getMessage());
            }
        });
        Result result = core.run(BUILTIN_TEST_CLASSES);
        System.out.println("[DeviceMockTCP] built-in tests: run=" + result.getRunCount()
                + " failures=" + result.getFailureCount()
                + " ignored=" + result.getIgnoreCount()
                + " time=" + result.getRunTime() + "ms"
                + " => " + (result.wasSuccessful() ? "PASS" : "FAIL"));
        if (!result.wasSuccessful()) {
            for (Failure failure : result.getFailures()) {
                System.err.println(failure.getTrace());
            }
            System.exit(1);
        }
    }

    private static void printLiveSummary(int sent, List<MockTransportMessage> messages) {
        Map<Integer, Integer> byType = new TreeMap<Integer, Integer>();
        int ok = 0;
        int empty = 0;
        int unsupported = 0;
        int failed = 0;
        int ack = 0;
        for (MockTransportMessage message : messages) {
            int type = message.getServiceType();
            Integer count = byType.get(type);
            byType.put(type, count == null ? 1 : count + 1);
            if (type == MockProtocolCodec.SERVICE_ACK) {
                ack++;
            }
            MockDecodeResult business = message.getBusiness();
            if (business == null) {
                failed++;
            } else {
                switch (business.getStatus()) {
                    case OK:
                        ok++;
                        break;
                    case EMPTY:
                        empty++;
                        break;
                    case UNSUPPORTED:
                        unsupported++;
                        break;
                    case FAILED:
                    default:
                        failed++;
                        break;
                }
            }
        }
        StringBuilder types = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : byType.entrySet()) {
            if (types.length() > 0) {
                types.append(", ");
            }
            types.append(hexType(entry.getKey())).append(" x").append(entry.getValue());
        }
        System.out.println("[DeviceMockTCP] SUMMARY sent=" + sent
                + " received=" + messages.size()
                + " ack=" + ack
                + " businessOK=" + ok
                + " businessEMPTY=" + empty
                + " businessUNSUPPORTED=" + unsupported
                + " businessFAILED=" + failed);
        System.out.println("[DeviceMockTCP] downlink by serviceType: "
                + (types.length() == 0 ? "none" : types.toString()));
        System.out.println("[DeviceMockTCP] note: 0x00 SERVICE_ACK is ProxyTcp's uplink ack; "
                + "publish platform.downlink to receive business downlinks");
    }

    private static String hexType(int serviceType) {
        return String.format("0x%02X", serviceType);
    }

    private static boolean codecUnsupported(int serviceType) {
        return serviceType == 96;
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  DeviceMockTCP                       run built-in full test suite (default)");
        System.out.println("  DeviceMockTCP --test                same as above");
        System.out.println("  DeviceMockTCP --live [host] [port]  connect a real ProxyTcp, send all 37 types");
        System.out.println("  DeviceMockTCP [host] [port] [options]  live device simulation");
        System.out.println("Options (live mode):");
        System.out.println("  --all                 send all 37 service types with schema samples");
        System.out.println("  --type=<code>          send one service type (decimal or 0xHEX), repeatable");
        System.out.println("  --json=<jsonObject>    use custom decoded JSON for --type");
        System.out.println("  --listen=<seconds>     keep receiving downlinks after sending (default "
                + DEFAULT_LISTEN_SECONDS + ")");
        System.out.println("  --device-id=<id>       override simulated deviceId (default "
                + DEFAULT_DEVICE_ID + ")");
        System.out.println("Examples:");
        System.out.println("  DeviceMockTCP                                   # 37-type codec + Netty closed-loop tests");
        System.out.println("  DeviceMockTCP --live --listen=30                 # all 37 types to 127.0.0.1:50001");
        System.out.println("  DeviceMockTCP 127.0.0.1 50001 --type=101 --listen=15");
        System.out.println("  DeviceMockTCP 127.0.0.1 50001 --all --listen=30");
        System.out.println("  DeviceMockTCP 127.0.0.1 50001 --type=101 --json={\"appType\":1,\"action\":9}");
    }
}
