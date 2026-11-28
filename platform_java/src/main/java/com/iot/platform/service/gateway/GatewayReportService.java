package com.iot.platform.service.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.GatewayConfigSnapshot;
import com.iot.platform.repository.gateway.GatewayConfigSnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** 网关上报快照查询服务。 */
@Service
public class GatewayReportService {

    private final GatewayConfigSnapshotRepository repository;

    public GatewayReportService(GatewayConfigSnapshotRepository repository) { this.repository = repository; }

    public List<GatewayConfigSnapshot> list(String gatewayCode, Integer serviceType, int limit) {
        var q = Wrappers.<GatewayConfigSnapshot>lambdaQuery();
        if (gatewayCode != null && !gatewayCode.isEmpty()) q.eq(GatewayConfigSnapshot::getGatewayCode, gatewayCode);
        if (serviceType != null) q.eq(GatewayConfigSnapshot::getServiceType, serviceType);
        q.orderByDesc(GatewayConfigSnapshot::getReportedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 500)));
        return repository.findAll(q);
    }
}
