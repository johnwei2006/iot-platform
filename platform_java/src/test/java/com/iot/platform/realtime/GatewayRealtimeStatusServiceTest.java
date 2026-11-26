package com.iot.platform.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.config.RealtimeProperties;
import com.iot.platform.model.GatewayRealtimeStatus;
import com.iot.platform.mq.UplinkMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GatewayRealtimeStatusServiceTest {

    @Mock
    private RedisGatewayStatusStore store;

    private final ObjectMapper mapper = new ObjectMapper();
    private RealtimeProperties properties;
    private GatewayRealtimeStatusService service;

    @BeforeEach
    public void setUp() {
        properties = new RealtimeProperties();
        service = new GatewayRealtimeStatusService(store, properties, mapper);
    }

    @Test
    public void buildsOnlineSnapshotWithLatestBusinessDataAndSensorSummary() throws Exception {
        UplinkMessage up = uplink("D1");
        up.setServiceDataDecoded(mapper.readTree("{\"nodeId\":\"N1\",\"sensors\":["
                + "{\"channel\":1,\"channelResult\":0,\"sensorValue\":23.5,"
                + "  \"sensorCode\":\"TEMP1\",\"sensorType\":\"1\",\"unit\":\"C\"},"
                + "{\"channel\":2,\"channelResult\":1,\"sensorValue\":99.0},"
                + "{\"channel\":3,\"channelResult\":0,\"sensorValue\":null},"
                + "{\"channelResult\":0,\"sensorValue\":1.0}]}"));

        service.refresh(up);

        GatewayRealtimeStatus status = captureWritten();
        assertEquals("D1", status.getDeviceId());
        assertEquals("N1", status.getNodeId());
        assertEquals("ONLINE", status.getStatus());
        assertEquals(24, status.getServiceType());
        assertEquals("GATEWAY_SENSOR_DATA_REPORT", status.getProtocolName());
        assertEquals("OK", status.getDecodeStatus());
        assertEquals(1001L, status.getEventId());
        assertEquals(120, status.getTtlSeconds());
        assertTrue(status.getLastSeen() > 0);
        assertNotNull(status.getLatestBusinessData());
        assertEquals(1, status.getSensors().size());
        assertEquals(1, status.getSensors().get(0).getChannel());
        assertEquals(23.5, status.getSensors().get(0).getSensorValue(), 0.0001);
        assertEquals("TEMP1", status.getSensors().get(0).getSensorCode());
        assertEquals("C", status.getSensors().get(0).getUnit());
    }

    @Test
    public void keepsOnlineStatusWhenStructuredDataIsAbsent() {
        UplinkMessage up = uplink("D2");
        up.setDecodeStatus("FAILED");

        service.refresh(up);

        GatewayRealtimeStatus status = captureWritten();
        assertEquals("ONLINE", status.getStatus());
        assertEquals("FAILED", status.getDecodeStatus());
        assertEquals("NODE-2", status.getNodeId());
        assertNull(status.getLatestBusinessData());
        assertNull(status.getSensors());
    }

    @Test
    public void skipsRefreshWhenDisabled() {
        properties.setEnabled(false);

        service.refresh(uplink("D3"));

        verify(store, never()).write(any(GatewayRealtimeStatus.class));
    }

    @Test
    public void skipsRefreshWhenDeviceIdIsBlank() {
        UplinkMessage up = uplink("   ");

        service.refresh(up);

        verify(store, never()).write(any(GatewayRealtimeStatus.class));
    }

    @Test
    public void storeFailureNeverEscapesFromRefresh() {
        when(store.write(any(GatewayRealtimeStatus.class)))
                .thenThrow(new RuntimeException("redis down"));

        service.refresh(uplink("D4"));

        verify(store).write(any(GatewayRealtimeStatus.class));
    }

    @Test
    public void nullMessageIsIgnored() {
        service.refresh(null);
        verify(store, never()).write(any(GatewayRealtimeStatus.class));
    }

    private GatewayRealtimeStatus captureWritten() {
        ArgumentCaptor<GatewayRealtimeStatus> captor =
                ArgumentCaptor.forClass(GatewayRealtimeStatus.class);
        verify(store).write(captor.capture());
        return captor.getValue();
    }

    private UplinkMessage uplink(String deviceId) {
        UplinkMessage up = new UplinkMessage();
        up.setDeviceId(deviceId);
        up.setNodeId("NODE-2");
        up.setServiceType(24);
        up.setServiceVersion(1);
        up.setServiceData("AQ==");
        up.setEventId(1001L);
        up.setTimestamp(1700000000L);
        up.setProtocolName("GATEWAY_SENSOR_DATA_REPORT");
        up.setTransactionType("uplink");
        up.setDecodeStatus("OK");
        return up;
    }
}
