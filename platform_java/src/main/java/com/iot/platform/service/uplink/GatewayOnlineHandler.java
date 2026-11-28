package com.iot.platform.service.uplink;

import com.iot.platform.model.Gateway;
import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.repository.gateway.GatewayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 主链路步骤 2：刷新设备在线状态。
 *
 * <p>设备首次出现时创建记录（deviceId + createdAt），随后置 ONLINE 并刷新时间。
 * 注意：TTL 判离线由 Redis 实时状态承担，此处 MongoDB 仅保留最后一次在线快照。
 */
@Component
@Order(2)
public class GatewayOnlineHandler implements UplinkHandler {

    private final GatewayRepository deviceRepository;

    @Autowired
    public GatewayOnlineHandler(GatewayRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public String name() {
        return "deviceOnline";
    }

    @Override
    public boolean critical() {
        return true;
    }

    @Override
    public void handle(UplinkMessage message) {
        Gateway device = deviceRepository.findByDeviceId(message.getDeviceId()).orElseGet(() -> {
            Gateway created = new Gateway();
            created.setDeviceId(message.getDeviceId());
            created.setCreatedAt(new Date());
            return created;
        });
        Date now = new Date();
        device.setStatus("ONLINE");
        device.setLastSeen(now);
        device.setUpdatedAt(now);
        deviceRepository.save(device);
    }
}
