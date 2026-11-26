package com.iot.platform.service;

import com.iot.platform.mapper.DeviceCtrlMapper;
import com.iot.platform.mapper.GatewayMapper;
import com.iot.platform.mapper.NodeMapper;
import com.iot.platform.mapper.SensorMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AreaReferenceServiceTest {

    @Mock
    private GatewayMapper gatewayMapper;
    @Mock
    private NodeMapper nodeMapper;
    @Mock
    private SensorMapper sensorMapper;
    @Mock
    private DeviceCtrlMapper deviceCtrlMapper;

    @InjectMocks
    private AreaReferenceService service;

    @Test
    public void countsAllFourEntityTypes() {
        when(gatewayMapper.selectCount(any())).thenReturn(2L);
        when(nodeMapper.selectCount(any())).thenReturn(1L);
        when(sensorMapper.selectCount(any())).thenReturn(3L);
        when(deviceCtrlMapper.selectCount(any())).thenReturn(0L);

        Map<String, Long> counts = service.countByArea("area-1");

        assertEquals(2L, counts.get("gateway"));
        assertEquals(1L, counts.get("node"));
        assertEquals(3L, counts.get("sensor"));
        assertEquals(0L, counts.get("deviceCtrl"));
        assertEquals(6L, counts.get("total"));
        assertTrue(service.describe(counts).contains("网关 2"));
    }

    @Test
    public void blankAreaIdReturnsZeroCounts() {
        Map<String, Long> counts = service.countByArea("  ");
        assertEquals(0L, counts.get("total"));
        assertEquals("", service.describe(counts));
    }
}
