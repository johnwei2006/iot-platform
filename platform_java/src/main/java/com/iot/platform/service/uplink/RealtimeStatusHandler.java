package com.iot.platform.service.uplink;

import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.realtime.GatewayRealtimeStatusService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 旁路步骤 5：更新 Redis 设备实时状态（在线快照 + 最新业务数据，String JSON + TTL）。
 *
 * <p>best-effort：服务缺失、Redis 不可用或写入失败均由编排层降级处理，
 * 不影响上行 ACK 与其他步骤。
 */
@Component
@Order(5)
public class RealtimeStatusHandler implements UplinkHandler {

    private final GatewayRealtimeStatusService statusService;

    @Autowired
    public RealtimeStatusHandler(ObjectProvider<GatewayRealtimeStatusService> provider) {
        this.statusService = provider == null ? null : provider.getIfAvailable();
    }

    @Override
    public String name() {
        return "realtimeStatus";
    }

    @Override
    public boolean critical() {
        return false;
    }

    @Override
    public void handle(UplinkMessage message) {
        if (statusService != null) {
            statusService.refresh(message);
        }
    }
}
