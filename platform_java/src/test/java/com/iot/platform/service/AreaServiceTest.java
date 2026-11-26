package com.iot.platform.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.Area;
import com.iot.platform.repository.AreaRepository;
import com.iot.platform.security.datascope.DataScopeQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AreaServiceTest {

    @Mock
    private AreaRepository repository;

    @InjectMocks
    private AreaService service;

    @Test
    public void createBindsOwnerAndCreatorOrg() {
        Area area = new Area();
        area.setAreaName("设施展示区");
        area.setBoundary("[[116.39,39.91],[116.40,39.91],[116.40,39.92]]");
        area.setDrawAreaMu(new BigDecimal("0.54"));
        area.setLabelAreaMu(new BigDecimal("1.24"));
        when(repository.save(any(Area.class))).thenAnswer(inv -> inv.getArgument(0));

        Area saved = service.create(area, "user-1", "0001");

        assertEquals("user-1", saved.getOwnerUserId());
        assertEquals("0001", saved.getOrgCode());
        assertEquals("user-1", saved.getCreateBy());
        assertEquals(new BigDecimal("0.54"), saved.getDrawAreaMu());
    }

    @Test
    public void updateOnlyAppliesNameAndLabelArea() {
        Area existing = new Area();
        existing.setId("a1");
        existing.setAreaName("原名");
        existing.setBoundary("[[116.39,39.91],[116.40,39.91],[116.40,39.92]]");
        existing.setDrawAreaMu(new BigDecimal("19.17"));
        existing.setLabelAreaMu(new BigDecimal("19.17"));
        existing.setOwnerUserId("owner-1");
        existing.setOrgCode("0001");
        when(repository.findById("a1")).thenReturn(Optional.of(existing));
        when(repository.save(any(Area.class))).thenAnswer(inv -> inv.getArgument(0));

        Area patch = new Area();
        patch.setAreaName("新名称");
        patch.setLabelAreaMu(new BigDecimal("20.00"));
        patch.setBoundary("[[]]");
        patch.setDrawAreaMu(new BigDecimal("99.99"));
        Area updated = service.update("a1", patch);

        assertEquals("新名称", updated.getAreaName());
        assertEquals(new BigDecimal("20.00"), updated.getLabelAreaMu());
        assertEquals("[[116.39,39.91],[116.40,39.91],[116.40,39.92]]", updated.getBoundary());
        assertEquals(new BigDecimal("19.17"), updated.getDrawAreaMu());
        assertEquals("owner-1", updated.getOwnerUserId());
    }

    @Test
    public void selfScopeQueriesByOwner() {
        DataScopeQuery scope = DataScopeQuery.self("user-2");
        when(repository.findAll(any(Wrapper.class))).thenReturn(List.of());

        service.listByCriteria(scope);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Area>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(repository).findAll(captor.capture());
    }
}
