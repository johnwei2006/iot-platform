package com.iot.platform.service.uplink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.platform.model.Alarm;
import com.iot.platform.model.DataRecord;
import com.iot.platform.model.Device;
import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.repository.AlarmRepository;
import com.iot.platform.repository.DataRecordRepository;
import com.iot.platform.repository.DeviceRepository;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PrimaryHandlersTest {

    @Test
    public void dataRecordPersistMapsAllFields() {
        ObjectMapper mapper = new ObjectMapper();
        DataRecordRepository repository = mock(DataRecordRepository.class);
        UplinkMessage up = uplink("D1", 101);
        up.setServiceVersion(1);
        up.setServiceData("AQI=");
        up.setEventId(300);
        up.setProtocolName("GATEWAY_REBOOT");
        up.setTransactionType("downlink");
        up.setDecodeStatus("OK");
        up.setDecodeError("none");
        ObjectNode decoded = mapper.createObjectNode();
        decoded.put("appType", 1);
        up.setServiceDataDecoded(decoded);

        new DataRecordPersistHandler(mapper, repository).handle(up);

        ArgumentCaptor<DataRecord> captor = ArgumentCaptor.forClass(DataRecord.class);
        verify(repository).save(captor.capture());
        DataRecord saved = captor.getValue();
        assertEquals("D1", saved.getDeviceId());
        assertEquals(101, saved.getServiceType());
        assertEquals(300, saved.getEventId());
        assertEquals("AQI=", saved.getServiceData());
        assertEquals("GATEWAY_REBOOT", saved.getProtocolName());
        assertEquals("downlink", saved.getTransactionType());
        assertEquals("OK", saved.getDecodeStatus());
        assertEquals("none", saved.getDecodeError());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getServiceDataDecoded());
        assertEquals(Integer.valueOf(1), saved.getServiceDataDecoded().get("appType"));
    }

    @Test
    public void deviceOnlineCreatesNewDevice() {
        DeviceRepository repository = mock(DeviceRepository.class);
        when(repository.findByDeviceId("D2")).thenReturn(Optional.<Device>empty());
        when(repository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        new DeviceOnlineHandler(repository).handle(uplink("D2", 1));

        verify(repository).save(ArgumentMatchers.argThat((Device d) ->
                "D2".equals(d.getDeviceId()) && "ONLINE".equals(d.getStatus())
                        && d.getCreatedAt() != null && d.getLastSeen() != null
                        && d.getUpdatedAt() != null));
    }

    @Test
    public void deviceOnlineUpdatesExistingDevice() {
        DeviceRepository repository = mock(DeviceRepository.class);
        Device existing = new Device();
        existing.setDeviceId("D3");
        existing.setStatus("OFFLINE");
        when(repository.findByDeviceId("D3")).thenReturn(Optional.of(existing));
        when(repository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

        new DeviceOnlineHandler(repository).handle(uplink("D3", 1));

        verify(repository).save(ArgumentMatchers.argThat((Device d) ->
                d == existing && "ONLINE".equals(d.getStatus()) && d.getLastSeen() != null));
    }

    @Test
    public void alarmRuleCreatesAlarmForServiceType99() {
        AlarmRepository repository = mock(AlarmRepository.class);
        when(repository.save(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        new AlarmRuleHandler(repository).handle(uplink("D4", 99));

        verify(repository).save(ArgumentMatchers.argThat((Alarm a) ->
                "D4".equals(a.getDeviceId()) && "SERVICE_TYPE_99".equals(a.getRule())
                        && "ERROR".equals(a.getLevel()) && "OPEN".equals(a.getStatus())
                        && a.getCreatedAt() != null));
    }

    @Test
    public void alarmRuleIgnoresOtherServiceTypes() {
        AlarmRepository repository = mock(AlarmRepository.class);

        new AlarmRuleHandler(repository).handle(uplink("D5", 1));

        verify(repository, never()).save(any(Alarm.class));
    }

    @Test
    public void primaryHandlersAreCriticalAndNamed() {
        ObjectMapper mapper = new ObjectMapper();
        UplinkHandler record = new DataRecordPersistHandler(mapper, mock(DataRecordRepository.class));
        UplinkHandler device = new DeviceOnlineHandler(mock(DeviceRepository.class));
        UplinkHandler alarm = new AlarmRuleHandler(mock(AlarmRepository.class));

        assertEquals("dataRecordPersist", record.name());
        assertEquals("deviceOnline", device.name());
        assertEquals("alarmRule", alarm.name());
        assertTrue(record.critical());
        assertTrue(device.critical());
        assertTrue(alarm.critical());
    }

    private UplinkMessage uplink(String deviceId, int serviceType) {
        UplinkMessage up = new UplinkMessage();
        up.setDeviceId(deviceId);
        up.setServiceType(serviceType);
        return up;
    }
}
