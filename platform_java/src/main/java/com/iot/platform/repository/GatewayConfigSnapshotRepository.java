package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.GatewayConfigSnapshot;

import java.util.List;

public interface GatewayConfigSnapshotRepository {
    List<GatewayConfigSnapshot> findAll(Wrapper<GatewayConfigSnapshot> wrapper);

    /** 查询该网关当前未下发或下发失败的配置记录（业务上最多一条）。 */
    GatewayConfigSnapshot findPendingOrFailed(String gatewayCode);

    GatewayConfigSnapshot save(GatewayConfigSnapshot snapshot);

    void update(GatewayConfigSnapshot snapshot);
}
