package com.iot.platform.service;

import com.iot.platform.model.Alarm;
import com.iot.platform.repository.AlarmRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
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
public class AlarmServiceTest {

    @Mock
    private AlarmRepository alarmRepository;

    @InjectMocks
    private AlarmService alarmService;

    @Test
    public void resolveSetsResolvedAndTimestamp() {
        Alarm a = new Alarm();
        a.setId("a1");
        a.setStatus("OPEN");
        when(alarmRepository.findById("a1")).thenReturn(Optional.of(a));
        when(alarmRepository.save(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));

        Alarm r = alarmService.resolve("a1");

        assertEquals("RESOLVED", r.getStatus());
        assertNotNull(r.getResolvedAt());
    }

    @Test
    public void resolveThrowsWhenNotFound() {
        when(alarmRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> alarmService.resolve("missing"));
    }

    @Test
    public void listReturnsEmptyWhenNoStatus() {
        when(alarmRepository.findAll()).thenReturn(Collections.emptyList());
        assertTrue(alarmService.list(null).isEmpty());
    }
}
