package com.iot.platform.repository.gateway;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.GatewayCommandLog;

import java.util.List;

public interface GatewayCommandLogRepository {
    List<GatewayCommandLog> findAll(Wrapper<GatewayCommandLog> wrapper);
    GatewayCommandLog findByRequestId(String requestId);
    GatewayCommandLog save(GatewayCommandLog log);
}
