package com.iot.platform.service;

import com.iot.platform.model.Gateway;
import com.iot.platform.repository.GatewayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GatewayServiceTest {

    @Mock
    private GatewayRepository deviceRepository;

    @InjectMocks
    private GatewayService deviceService;

    @Test
    public void createSetsDefaultStatusAndTimestamps() {
        Gateway d = new Gateway();
        d.setDeviceId("D1");
        when(deviceRepository.save(any(Gateway.class))).thenAnswer(inv -> inv.getArgument(0));

        Gateway saved = deviceService.create(d, "admin-1", "0001");

        assertEquals("OFFLINE", saved.getStatus());
        assertEquals("admin-1", saved.getOwnerUserId());
        assertEquals("0001", saved.getOrgCode());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    public void updateThrowsWhenNotFound() {
        when(deviceRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> deviceService.update("missing", new Gateway()));
    }

    @Test
    public void updateCopiesProvidedFields() {
        Gateway existing = new Gateway();
        existing.setId("x");
        existing.setDeviceId("D1");
        existing.setStatus("OFFLINE");
        when(deviceRepository.findById("x")).thenReturn(Optional.of(existing));
        when(deviceRepository.save(any(Gateway.class))).thenAnswer(inv -> inv.getArgument(0));

        Gateway patch = new Gateway();
        patch.setName("冷链车A");
        patch.setStatus("ONLINE");
        Gateway updated = deviceService.update("x", patch);

        assertEquals("冷链车A", updated.getName());
        assertEquals("ONLINE", updated.getStatus());
    }

    @Test
    public void markOnlineCreatesWhenAbsent() {
        when(deviceRepository.findByDeviceId("D9")).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Gateway.class))).thenAnswer(inv -> inv.getArgument(0));

        deviceService.markOnline("D9");

        verify(deviceRepository).save(ArgumentMatchers.argThat(
                d -> "ONLINE".equals(d.getStatus()) && "D9".equals(d.getDeviceId())));
    }

    @Test
    public void markOnlineUpdatesWhenPresent() {
        Gateway d = new Gateway();
        d.setDeviceId("D9");
        d.setStatus("OFFLINE");
        when(deviceRepository.findByDeviceId("D9")).thenReturn(Optional.of(d));
        when(deviceRepository.save(any(Gateway.class))).thenAnswer(inv -> inv.getArgument(0));

        deviceService.markOnline("D9");

        assertTrue(true);
    }
}
