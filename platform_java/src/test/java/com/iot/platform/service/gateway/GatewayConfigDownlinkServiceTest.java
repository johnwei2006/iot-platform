package com.iot.platform.service.gateway;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.constant.GatewayProtocolConstants;
import com.iot.platform.mq.DownlinkMessage;
import com.iot.platform.model.GatewayCommandLog;
import com.iot.platform.repository.GatewayCommandLogRepository;
import com.iot.platform.service.DownlinkService;
import com.iot.platform.service.downlink.DownlinkHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GatewayConfigDownlinkServiceTest {

    @Test
    public void sendsConfigWithProtocolServiceTypeAndLogsCommand() {
        CapturingHandler handler = new CapturingHandler();
        MemoryLogRepository repository = new MemoryLogRepository();
        GatewayConfigDownlinkService service = new GatewayConfigDownlinkService(
                new DownlinkService(List.of(handler)), repository);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("loraFreqGroup", 2);

        GatewayCommandLog log = service.send("GW1", "GW1", GatewayProtocolConstants.GATEWAY_CONFIG,
                "RADIO", payload, "req-1", "tester");

        assertEquals(1, handler.messages.size());
        DownlinkMessage sent = handler.messages.get(0);
        assertEquals("GW1", sent.getDeviceId());
        assertEquals(9, sent.getServiceType());
        assertNotNull(sent.getServiceDataDecoded());
        assertEquals(2, sent.getServiceDataDecoded().path("loraFreqGroup").asInt());
        assertEquals(GatewayCommandLog.PENDING, log.getStatus());
        assertSame(log, repository.byRequest.get("req-1"));
    }

    @Test
    public void reusesCommandForSameRequestId() {
        CapturingHandler handler = new CapturingHandler();
        MemoryLogRepository repository = new MemoryLogRepository();
        GatewayConfigDownlinkService service = new GatewayConfigDownlinkService(
                new DownlinkService(List.of(handler)), repository);

        GatewayCommandLog first = service.reboot("GW1", "GW1", 1, "reboot-1", "tester");
        GatewayCommandLog second = service.reboot("GW1", "GW1", 1, "reboot-1", "tester");

        assertSame(first, second);
        assertEquals(1, handler.messages.size());
        assertEquals(101, handler.messages.get(0).getServiceType());
        assertTrue(handler.messages.get(0).getServiceDataDecoded().path("action").asInt() == 1);
    }

    private static class CapturingHandler implements DownlinkHandler {
        private final List<DownlinkMessage> messages = new ArrayList<>();

        @Override
        public String name() { return "capture"; }

        @Override
        public void handle(DownlinkMessage message) { messages.add(message); }
    }

    private static class MemoryLogRepository implements GatewayCommandLogRepository {
        private final Map<String, GatewayCommandLog> byId = new LinkedHashMap<>();
        private final Map<String, GatewayCommandLog> byRequest = new LinkedHashMap<>();

        @Override
        public List<GatewayCommandLog> findAll(Wrapper<GatewayCommandLog> wrapper) {
            return new ArrayList<>(byId.values());
        }

        @Override
        public GatewayCommandLog findByRequestId(String requestId) { return byRequest.get(requestId); }

        @Override
        public GatewayCommandLog save(GatewayCommandLog log) {
            byId.put(log.getId(), log);
            if (log.getRequestId() != null) byRequest.put(log.getRequestId(), log);
            return log;
        }
    }
}
