package com.iot.platform.service.gateway;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.model.GatewayConfigSnapshot;
import com.iot.platform.repository.GatewayConfigSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GatewayConfigServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void keepsOnlyOnePendingOrFailedConfigPerGateway() {
        MemoryRepository repository = new MemoryRepository();
        GatewayConfigService service = new GatewayConfigService(repository);

        service.savePending("GW1", Map.of("configVersion", 1, "projectId", "P1"));
        service.savePending("GW1", Map.of("configVersion", 2, "projectId", "P2", "heartbeatInterval", 60));

        assertEquals(1, repository.items.size());
        GatewayConfigSnapshot record = repository.items.get(0);
        assertEquals(Integer.valueOf(2), record.getConfigVersion());
        assertEquals("P2", record.getProjectId());
        assertEquals(Integer.valueOf(60), record.getHeartbeatInterval());
        assertEquals(GatewayConfigSnapshot.STATUS_PENDING, record.getStatus());
        assertNotNull(record.getConfigTime());

        service.markFailed("GW1");
        assertEquals(GatewayConfigSnapshot.STATUS_FAILED, record.getStatus());

        service.savePending("GW1", Map.of("configVersion", 3));
        assertEquals(1, repository.items.size());
        assertEquals(GatewayConfigSnapshot.STATUS_PENDING, record.getStatus());
        assertEquals(Integer.valueOf(3), record.getConfigVersion());
    }

    @Test
    public void upstreamReportConfirmsPendingConfigWithoutAddingAnother() throws Exception {
        MemoryRepository repository = new MemoryRepository();
        GatewayConfigService service = new GatewayConfigService(repository);
        service.savePending("GW1", Map.of("configVersion", 1, "projectId", "P1"));
        service.markSent("GW1");
        assertNotNull(repository.items.get(0).getSendTime());

        service.applyUpstream("GW1", mapper.readTree(
                "{\"configVersion\":1,\"projectId\":\"P1\",\"reportInterval\":120}"), new Date());

        assertEquals(1, repository.items.size());
        assertEquals(GatewayConfigSnapshot.STATUS_SUCCESS, repository.items.get(0).getStatus());
        assertEquals(Integer.valueOf(120), repository.items.get(0).getReportInterval());
    }

    private static class MemoryRepository implements GatewayConfigSnapshotRepository {
        private final List<GatewayConfigSnapshot> items = new ArrayList<>();

        @Override
        public List<GatewayConfigSnapshot> findAll(Wrapper<GatewayConfigSnapshot> wrapper) { return items; }

        @Override
        public GatewayConfigSnapshot findPendingOrFailed(String gatewayCode) {
            return items.stream()
                    .filter(item -> gatewayCode.equals(item.getGatewayCode()))
                    .filter(item -> GatewayConfigSnapshot.STATUS_PENDING.equals(item.getStatus())
                            || GatewayConfigSnapshot.STATUS_FAILED.equals(item.getStatus()))
                    .findFirst().orElse(null);
        }

        @Override
        public GatewayConfigSnapshot save(GatewayConfigSnapshot snapshot) {
            items.add(snapshot);
            return snapshot;
        }

        @Override
        public void update(GatewayConfigSnapshot snapshot) { }
    }
}
