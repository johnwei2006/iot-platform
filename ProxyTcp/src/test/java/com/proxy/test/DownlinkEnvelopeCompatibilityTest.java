package com.proxy.test;

import com.proxy.mq.PlatformDownlinkReceiver;
import com.proxy.model.DownlinkTransportMessage;
import com.proxy.model.ServiceRequest;
import com.proxy.model.ServiceResponse;
import com.proxy.transport.TransportDownlinkProcessor;
import com.proxy.utils.JsonUtils;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 三端契约回归：dtm 输出到 proxy.downlink 的「标准 ServiceRequest JSON」必须能被
 * ProxyTcp 的 Gson（base64 byte[] 适配器）与 PlatformDownlinkReceiver 正确消费。
 */
public class DownlinkEnvelopeCompatibilityTest {

    private static final String DTM_OUTPUT_JSON = "{"
            + "\"deviceId\":\"30410007102\","
            + "\"nodeId\":\"\","
            + "\"eventId\":1,"
            + "\"serviceType\":101,"
            + "\"serviceVersion\":1,"
            + "\"serviceDataEncoding\":2,"
            + "\"serviceDataProtocolVersion\":1,"
            + "\"serviceEncryption\":0,"
            + "\"serviceData\":\"AQk=\""
            + "}";

    @Test
    public void dtmOutputParsesIntoServiceRequestWithBinaryPayload() {
        ServiceRequest req = JsonUtils.gson().fromJson(DTM_OUTPUT_JSON, ServiceRequest.class);
        assertEquals("30410007102", req.getDeviceId());
        assertEquals(101, req.getServiceType());
        assertArrayEquals(new byte[]{1, 9}, req.getServiceData());
    }

    @Test
    public void dtmOutputIsDispatchedByDownlinkReceiver() throws Exception {
        final DownlinkTransportMessage[] captured = {null};
        TransportDownlinkProcessor stub = new TransportDownlinkProcessor() {
            @Override
            public ServiceResponse process(DownlinkTransportMessage msg) {
                captured[0] = msg;
                return new ServiceResponse();
            }
        };
        PlatformDownlinkReceiver receiver = new PlatformDownlinkReceiver();
        Field field = receiver.getClass().getDeclaredField("transportDownlinkProcessor");
        field.setAccessible(true);
        field.set(receiver, stub);

        receiver.handleDownlink(DTM_OUTPUT_JSON);

        assertNotNull(captured[0]);
        assertEquals("30410007102", captured[0].getDeviceId());
        assertArrayEquals(new byte[]{1, 9}, captured[0].getServiceData());
    }

    @Test
    public void extensionFieldsFromPlatformAreIgnoredByGson() {
        String withExtensions = DTM_OUTPUT_JSON.substring(0, DTM_OUTPUT_JSON.length() - 1)
                + ",\"serviceDataDecoded\":{\"appType\":1},\"protocolName\":\"GATEWAY_REBOOT\","
                + "\"transactionType\":\"downlink\",\"decodeStatus\":\"OK\"}";
        ServiceRequest req = JsonUtils.gson().fromJson(withExtensions, ServiceRequest.class);
        assertEquals(101, req.getServiceType());
        assertArrayEquals(new byte[]{1, 9}, req.getServiceData());
    }

    @Test
    public void emptyPayloadProtocolUsesEmptyBase64String() {
        String json = "{\"deviceId\":\"g1\",\"serviceType\":1,\"serviceData\":\"\"}";
        ServiceRequest req = JsonUtils.gson().fromJson(json, ServiceRequest.class);
        assertNotNull(req.getServiceData());
        assertEquals(0, req.getServiceData().length);
    }
}
