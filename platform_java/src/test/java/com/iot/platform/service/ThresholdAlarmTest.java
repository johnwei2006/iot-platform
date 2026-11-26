package com.iot.platform.service;

import com.iot.platform.model.Alarm;
import com.iot.platform.model.MetricThreshold;
import com.iot.platform.repository.AlarmRepository;
import com.iot.platform.repository.MetricThresholdRepository;
import com.iot.platform.repository.MessageRepository;
import com.iot.platform.service.converter.SensorMetadataRegistry;
import com.iot.platform.service.uplink.ThresholdAlarmHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 阈值域单测：超限判定与超限告警联动（去重 + 站内消息）。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ThresholdAlarmTest {

    @Mock
    private MetricThresholdRepository thresholdRepository;

    @Mock
    private AlarmRepository alarmRepository;

    @Mock
    private MessageRepository messageRepository;

    private ThresholdAlarmHandler handler() {
        return new ThresholdAlarmHandler(thresholdRepository, alarmRepository,
                new SensorMetadataRegistry(), messageRepository, null, null);
    }

    private MetricThreshold threshold(double min, double max) {
        MetricThreshold t = new MetricThreshold();
        t.setId("th1");
        t.setDeviceId("GW1");
        t.setMetric("temp");
        t.setMinValue(BigDecimal.valueOf(min));
        t.setMaxValue(BigDecimal.valueOf(max));
        t.setUpdatedBy("admin-1");
        return t;
    }

    @Test
    void exceedsChecksBothBounds() {
        MetricThreshold t = threshold(12, 34);
        assertTrue(ThresholdService.exceeds(t, 36.0));
        assertTrue(ThresholdService.exceeds(t, 10.0));
        assertFalse(ThresholdService.exceeds(t, 26.5));
        assertFalse(ThresholdService.exceeds(null, 36.0));
    }

    @Test
    void evaluateCreatesAlarmAndNotifiesOwnerOnce() {
        when(thresholdRepository.findAll(any())).thenReturn(List.of(threshold(12, 34)));
        when(alarmRepository.findAll()).thenReturn(List.of());
        when(alarmRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        handler().evaluate("GW1", "temp", 36.0);

        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmRepository).save(captor.capture());
        assertEquals("OPEN", captor.getValue().getStatus());
        assertEquals("threshold:GW1:temp", captor.getValue().getRule());
        verify(messageRepository).save(any());

        // 已有未恢复同因告警时去重
        when(alarmRepository.findAll()).thenReturn(List.of(captor.getValue()));
        handler().evaluate("GW1", "temp", 37.0);
        verify(alarmRepository).save(any());
    }
}