package com.iot.platform.service;

import com.iot.platform.model.DeviceCtrl;
import com.iot.platform.repository.DeviceCtrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DeviceCtrlServiceTest {

    @Mock
    private DeviceCtrlRepository deviceCtrlRepository;

    @InjectMocks
    private DeviceCtrlService deviceCtrlService;

    @Test
    public void updatePartialFieldsKeepsUpdateByWhenMissing() {
        DeviceCtrl existing = new DeviceCtrl();
        existing.setId("d1");
        existing.setDeviceCode("DEV-1");
        existing.setUpdateBy("creator-1");
        when(deviceCtrlRepository.findById("d1")).thenReturn(Optional.of(existing));
        when(deviceCtrlRepository.save(any(DeviceCtrl.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceCtrl patch = new DeviceCtrl();
        patch.setDeviceName("新名称");
        patch.setStatus("0");
        patch.setLongitude("120.15");
        patch.setLatitude("30.28");

        DeviceCtrl updated = deviceCtrlService.update("d1", patch);

        assertEquals("新名称", updated.getDeviceName());
        assertEquals("0", updated.getStatus());
        assertEquals("120.15", updated.getLongitude());
        assertEquals("30.28", updated.getLatitude());
        assertEquals("creator-1", updated.getUpdateBy());
        assertNotNull(updated.getUpdateTime());
    }

    @Test
    public void updateAppliesExplicitUpdateBy() {
        DeviceCtrl existing = new DeviceCtrl();
        existing.setId("d2");
        existing.setUpdateBy("creator-2");
        when(deviceCtrlRepository.findById("d2")).thenReturn(Optional.of(existing));
        when(deviceCtrlRepository.save(any(DeviceCtrl.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceCtrl patch = new DeviceCtrl();
        patch.setUpdateBy("editor-9");

        assertEquals("editor-9", deviceCtrlService.update("d2", patch).getUpdateBy());
    }
}
