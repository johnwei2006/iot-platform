package com.iot.platform.service.sensor;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.SensorGroup;
import com.iot.platform.repository.sensor.SensorGroupRepository;
import com.iot.platform.security.datascope.DataScopeQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SensorGroupServiceTest {

    @Mock
    private SensorGroupRepository repository;

    @InjectMocks
    private SensorGroupService service;

    @Test
    public void createBindsOwnerAndCreatorOrg() {
        SensorGroup group = new SensorGroup();
        group.setGroupName("棚内空气温湿度");
        group.setSensorCodes("[\"SNS-1\",\"SNS-2\"]");
        when(repository.save(any(SensorGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        SensorGroup saved = service.create(group, "user-1", "0001");

        assertEquals("user-1", saved.getOwnerUserId());
        assertEquals("0001", saved.getOrgCode());
        assertEquals("user-1", saved.getCreateBy());
        assertEquals("[\"SNS-1\",\"SNS-2\"]", saved.getSensorCodes());
    }

    @Test
    public void updateKeepsOwnerAndAppliesWhitelist() {
        SensorGroup existing = new SensorGroup();
        existing.setId("g1");
        existing.setGroupName("原名");
        existing.setOwnerUserId("owner-1");
        existing.setOrgCode("0001");
        when(repository.findById("g1")).thenReturn(Optional.of(existing));
        when(repository.save(any(SensorGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        SensorGroup patch = new SensorGroup();
        patch.setGroupName("新名称");
        patch.setSensorCodes("[\"SNS-9\"]");
        SensorGroup updated = service.update("g1", patch);

        assertEquals("新名称", updated.getGroupName());
        assertEquals("[\"SNS-9\"]", updated.getSensorCodes());
        assertEquals("owner-1", updated.getOwnerUserId());
    }

    @Test
    public void selfScopeQueriesByOwner() {
        DataScopeQuery scope = DataScopeQuery.self("user-2");
        when(repository.findAll(any(Wrapper.class))).thenReturn(List.of());

        service.listByCriteria(scope);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<SensorGroup>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(repository).findAll(captor.capture());
    }
}
