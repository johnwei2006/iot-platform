package com.proxy.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.proxy.mq.PlatformDownlinkReceiver;
import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.ServiceResponse;
import com.proxy.transport.TransportDownlinkProcessor;

/**
 * 平台来源下行消费者单元测试（无需 broker / DB）。
 * 验证：DownlinkMessage 风格 JSON（字段对齐 ServiceRequest）→ handleDownlink()
 * 正确解析并派发到 TransportDownlinkProcessor，且 serviceData 的 base64 字符串
 * 经 Gson 反序列化为 byte[]。
 */
public class PlatformDownlinkReceiverTest {

    // 与 platform_java DownlinkMessage / ProxyTcp ServiceRequest 对齐的 JSON：
    // serviceData 为 base64 字符串（"QUJD" = "ABC"），Gson 应反序列化为 byte[]{65,66,67}
    private static final String DOWNLINK_JSON = "{"
            + "\"deviceId\":\"30410007102\","
            + "\"nodeId\":\"node01\","
            + "\"eventId\":7,"
            + "\"serviceType\":3,"
            + "\"serviceVersion\":1,"
            + "\"serviceDataEncoding\":0,"
            + "\"serviceDataProtocolVersion\":1,"
            + "\"serviceEncryption\":0,"
            + "\"serviceData\":\"QUJD\""
            + "}";

    @Test
    public void handleDownlinkParsesAndDispatches() throws Exception {
        final DownlinkTransportMessage[] captured = {null};
        TransportDownlinkProcessor stub = new TransportDownlinkProcessor() {
            @Override
            public ServiceResponse process(DownlinkTransportMessage msg) {
                captured[0] = msg;
                return new ServiceResponse();
            }
        };

        PlatformDownlinkReceiver receiver = new PlatformDownlinkReceiver();
        setField(receiver, "transportDownlinkProcessor", stub);

        receiver.handleDownlink(DOWNLINK_JSON);

        assertNotNull(captured[0], "process 应被调用");
        assertEquals("30410007102", captured[0].getDeviceId(), "deviceId 应映射");
        assertEquals("node01", captured[0].getNodeId(), "nodeId 应映射");
        assertEquals(3, captured[0].getServiceType(), "serviceType 应映射");
        assertEquals((short) 1, captured[0].getServiceVersion(), "serviceVersion 应映射");
        assertNotNull(captured[0].getServiceData(), "serviceData 应反序列化为 byte[]");
        assertEquals("ABC", new String(captured[0].getServiceData(), "UTF-8"),
                "serviceData base64->byte[] 应为 ABC");
    }

    @Test
    public void handleDownlinkInvalidJsonIsIgnored() throws Exception {
        final boolean[] called = {false};
        TransportDownlinkProcessor stub = new TransportDownlinkProcessor() {
            @Override
            public ServiceResponse process(DownlinkTransportMessage msg) {
                called[0] = true;
                return new ServiceResponse();
            }
        };
        PlatformDownlinkReceiver receiver = new PlatformDownlinkReceiver();
        setField(receiver, "transportDownlinkProcessor", stub);

        receiver.handleDownlink("not-a-json{");
        assertTrue(!called[0], "非法 JSON 不应派发 process");
    }

    @Test
    public void handleDownlinkMissingDeviceIdIsIgnored() throws Exception {
        final boolean[] called = {false};
        TransportDownlinkProcessor stub = new TransportDownlinkProcessor() {
            @Override
            public ServiceResponse process(DownlinkTransportMessage msg) {
                called[0] = true;
                return new ServiceResponse();
            }
        };
        PlatformDownlinkReceiver receiver = new PlatformDownlinkReceiver();
        setField(receiver, "transportDownlinkProcessor", stub);

        receiver.handleDownlink("{\"serviceType\":1}");
        assertTrue(!called[0], "缺少 deviceId 不应派发 process");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
