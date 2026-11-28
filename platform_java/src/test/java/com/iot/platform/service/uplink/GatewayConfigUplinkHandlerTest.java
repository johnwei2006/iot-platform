package com.iot.platform.service.uplink;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.constant.GatewayProtocolConstants;
import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.model.Gateway;
import com.iot.platform.model.GatewayCommandLog;
import com.iot.platform.model.GatewayConfigSnapshot;
import com.iot.platform.model.GatewayInfoHistory;
import com.iot.platform.model.GatewayTask;
import com.iot.platform.repository.GatewayCommandLogRepository;
import com.iot.platform.repository.GatewayConfigSnapshotRepository;
import com.iot.platform.repository.GatewayInfoHistoryRepository;
import com.iot.platform.repository.GatewayRepository;
import com.iot.platform.repository.GatewayTaskRepository;
import com.iot.platform.service.gateway.GatewayConfigService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class GatewayConfigUplinkHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void persistsGatewayInfoReportSnapshot() throws Exception {
        MemorySnapshotRepository snapshots = new MemorySnapshotRepository();
        MemoryInfoHistoryRepository infoHistory = new MemoryInfoHistoryRepository();
        MemoryGatewayRepository gateways = new MemoryGatewayRepository();
        Gateway gateway = new Gateway();
        gateway.setId("G1");
        gateway.setDeviceId("GW1");
        gateways.save(gateway);
        GatewayConfigUplinkHandler handler = new GatewayConfigUplinkHandler(snapshots,
                new MemoryCommandRepository(), new MemoryTaskRepository(), gateways, infoHistory,
                new GatewayConfigService(snapshots));
        UplinkMessage message = new UplinkMessage();
        message.setDeviceId("GW1");
        message.setServiceType(GatewayProtocolConstants.GATEWAY_INFO_REPORT);
        message.setTimestamp(1_700_000_000_000L);
        message.setServiceDataDecoded(mapper.readTree("{\"configVersion\":3,\"gatewayId\":\"GW-009\","
                + "\"heartbeatInterval\":60,\"firmwareVersion\":\"010203\",\"adapterExists\":1}"));

        handler.handle(message);

        assertEquals(1, snapshots.items.size());
        assertEquals("INFO", snapshots.items.get(0).getCategory());
        assertEquals("GW1", snapshots.items.get(0).getGatewayCode());
        assertEquals(1, infoHistory.items.size());
        assertEquals("GW1", infoHistory.items.get(0).getGatewayCode());
        assertEquals(Integer.valueOf(3), infoHistory.items.get(0).getConfigVersion());
        assertEquals("GW-009", infoHistory.items.get(0).getGatewayId());
        assertEquals(Integer.valueOf(60), infoHistory.items.get(0).getHeartbeatInterval());
        assertEquals(Integer.valueOf(1), infoHistory.items.get(0).getAdapterExists());
        assertEquals(Integer.valueOf(3), gateway.getConfigVersion());
        assertEquals("GW-009", gateway.getGatewayId());
        assertEquals("010203", gateway.getFirmwareVersion());
        assertEquals(new Date(1_700_000_000_000L), gateway.getInfoReportedAt());
    }

    @Test
    public void updatesTaskFeedbackFromTaskListResponse() throws Exception {
        MemoryTaskRepository tasks = new MemoryTaskRepository();
        GatewayTask task = new GatewayTask();
        task.setId("T1");
        task.setGatewayCode("GW1");
        task.setTaskNo(1);
        task.setDeployStatus("PENDING");
        tasks.save(task);
        MemorySnapshotRepository snapshots = new MemorySnapshotRepository();
        GatewayConfigUplinkHandler handler = new GatewayConfigUplinkHandler(
                snapshots, new MemoryCommandRepository(), tasks,
                new MemoryGatewayRepository(), new MemoryInfoHistoryRepository(),
                new GatewayConfigService(snapshots));
        UplinkMessage message = new UplinkMessage();
        message.setDeviceId("GW1");
        message.setServiceType(GatewayProtocolConstants.GATEWAY_CONFIG_TASK_LIST_RESPONSE);
        message.setServiceDataDecoded(mapper.readTree(
                "{\"taskCount\":1,\"tasks\":[{\"taskId\":1,\"result\":1,\"message\":\"bad task\"}]}"));

        handler.handle(message);

        GatewayTask updated = tasks.items.get("T1");
        assertEquals("FAILED", updated.getDeployStatus());
        assertEquals("bad task", updated.getFeedbackMsg());
        assertFalse(handler.critical());
    }

    private static class MemoryGatewayRepository implements GatewayRepository {
        private final Map<String, Gateway> items = new LinkedHashMap<>();

        @Override
        public Optional<Gateway> findByDeviceId(String deviceId) {
            return items.values().stream().filter(g -> deviceId.equals(g.getDeviceId())).findFirst();
        }

        @Override
        public List<Gateway> findByStatus(String status) {
            return new ArrayList<>(items.values());
        }

        @Override
        public List<Gateway> findAll() { return new ArrayList<>(items.values()); }

        @Override
        public List<Gateway> findAll(Wrapper<Gateway> wrapper) { return new ArrayList<>(items.values()); }

        @Override
        public Optional<Gateway> findById(String id) { return Optional.ofNullable(items.get(id)); }

        @Override
        public Gateway save(Gateway gateway) {
            items.put(gateway.getId(), gateway);
            return gateway;
        }

        @Override
        public void deleteById(String id) { items.remove(id); }
    }

    private static class MemoryInfoHistoryRepository implements GatewayInfoHistoryRepository {
        private final List<GatewayInfoHistory> items = new ArrayList<>();

        @Override
        public List<GatewayInfoHistory> findAll(Wrapper<GatewayInfoHistory> wrapper) { return items; }

        @Override
        public IPage<GatewayInfoHistory> findPage(Page<GatewayInfoHistory> page, Wrapper<GatewayInfoHistory> wrapper) {
            return page;
        }

        @Override
        public GatewayInfoHistory save(GatewayInfoHistory heartbeat) {
            items.add(heartbeat);
            return heartbeat;
        }
    }

    private static class MemorySnapshotRepository implements GatewayConfigSnapshotRepository {
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

    private static class MemoryCommandRepository implements GatewayCommandLogRepository {
        private final Map<String, GatewayCommandLog> items = new LinkedHashMap<>();

        @Override
        public List<GatewayCommandLog> findAll(Wrapper<GatewayCommandLog> wrapper) {
            return new ArrayList<>(items.values());
        }

        @Override
        public GatewayCommandLog findByRequestId(String requestId) { return items.get(requestId); }

        @Override
        public GatewayCommandLog save(GatewayCommandLog log) {
            items.put(log.getId(), log);
            return log;
        }
    }

    private static class MemoryTaskRepository implements GatewayTaskRepository {
        private final Map<String, GatewayTask> items = new LinkedHashMap<>();

        @Override
        public List<GatewayTask> findAll(Wrapper<GatewayTask> wrapper) { return new ArrayList<>(items.values()); }

        @Override
        public GatewayTask findById(String id) { return items.get(id); }

        @Override
        public GatewayTask save(GatewayTask task) {
            items.put(task.getId(), task);
            return task;
        }

        @Override
        public void deleteById(String id) { items.remove(id); }
    }
}
