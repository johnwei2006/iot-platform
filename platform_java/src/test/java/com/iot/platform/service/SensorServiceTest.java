package com.iot.platform.service;

import com.iot.platform.config.TdEngineProperties;
import com.iot.platform.model.Sensor;
import com.iot.platform.model.SensorReading;
import com.iot.platform.repository.SensorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SensorServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private JdbcTemplate tdEngineJdbcTemplate;

    @Mock
    private TdEngineProperties tdEngineProperties;

    @InjectMocks
    private SensorService sensorService;

    @Test
    public void updatePartialFieldsKeepsUpdateByWhenMissing() {
        Sensor existing = new Sensor();
        existing.setId("s1");
        existing.setSensorCode("SNS-1");
        existing.setUpdateBy("creator-1");
        when(sensorRepository.findById("s1")).thenReturn(Optional.of(existing));
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(inv -> inv.getArgument(0));

        Sensor patch = new Sensor();
        patch.setSensorName("新名称");
        patch.setAreaId("area-1");
        patch.setWarnMin(10.0);
        patch.setWarnMax(40.0);

        Sensor updated = sensorService.update("s1", patch);

        assertEquals("新名称", updated.getSensorName());
        assertEquals("area-1", updated.getAreaId());
        assertEquals(10.0, updated.getWarnMin());
        assertEquals(40.0, updated.getWarnMax());
        assertEquals("creator-1", updated.getUpdateBy());
    }

    @Test
    public void updateAppliesExplicitUpdateBy() {
        Sensor existing = new Sensor();
        existing.setId("s2");
        existing.setUpdateBy("creator-2");
        when(sensorRepository.findById("s2")).thenReturn(Optional.of(existing));
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(inv -> inv.getArgument(0));

        Sensor patch = new Sensor();
        patch.setUpdateBy("editor-9");

        assertEquals("editor-9", sensorService.update("s2", patch).getUpdateBy());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void historyMapsSensorReadings() throws Exception {
        when(tdEngineProperties.getDatabase()).thenReturn("iot");
        when(tdEngineProperties.getStable()).thenReturn("sensordata");
        when(tdEngineJdbcTemplate.query(anyString(), any(RowMapper.class),
                eq("SNS-1"), any(java.sql.Timestamp.class), any(java.sql.Timestamp.class)))
                .thenAnswer(inv -> {
                    RowMapper<SensorReading> mapper = inv.getArgument(1);
                    ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
                    when(rs.getLong(1)).thenReturn(1700000000000L);
                    when(rs.getDouble(2)).thenReturn(23.5);
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<SensorReading> readings = sensorService.history("SNS-1", 1000L, 2000L);

        assertEquals(1, readings.size());
        assertEquals(1700000000000L, readings.get(0).getTs());
        assertEquals(23.5, readings.get(0).getSenval());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void historyFallsBackToEmptyWhenTdEngineFails() {
        when(tdEngineProperties.getDatabase()).thenReturn("iot");
        when(tdEngineProperties.getStable()).thenReturn("sensordata");
        when(tdEngineJdbcTemplate.query(anyString(), any(RowMapper.class),
                anyString(), any(java.sql.Timestamp.class), any(java.sql.Timestamp.class)))
                .thenThrow(new RuntimeException("tdengine unavailable"));

        assertTrue(sensorService.history("SNS-1", 1000L, 2000L).isEmpty());
    }
}
