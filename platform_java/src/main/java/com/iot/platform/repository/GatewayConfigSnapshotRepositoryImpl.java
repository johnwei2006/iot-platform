package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.GatewayConfigSnapshotMapper;
import com.iot.platform.model.GatewayConfigSnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class GatewayConfigSnapshotRepositoryImpl implements GatewayConfigSnapshotRepository {

    private final GatewayConfigSnapshotMapper mapper;

    public GatewayConfigSnapshotRepositoryImpl(GatewayConfigSnapshotMapper mapper) { this.mapper = mapper; }

    @Override
    public List<GatewayConfigSnapshot> findAll(Wrapper<GatewayConfigSnapshot> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public GatewayConfigSnapshot findPendingOrFailed(String gatewayCode) {
        List<GatewayConfigSnapshot> records = mapper.selectList(Wrappers.<GatewayConfigSnapshot>lambdaQuery()
                .eq(GatewayConfigSnapshot::getGatewayCode, gatewayCode)
                .in(GatewayConfigSnapshot::getStatus,
                        GatewayConfigSnapshot.STATUS_PENDING, GatewayConfigSnapshot.STATUS_FAILED)
                .orderByDesc(GatewayConfigSnapshot::getConfigTime));
        return records.isEmpty() ? null : records.get(0);
    }

    @Override
    public GatewayConfigSnapshot save(GatewayConfigSnapshot snapshot) {
        if (snapshot.getId() == null || snapshot.getId().isEmpty()) snapshot.setId(UUID.randomUUID().toString());
        mapper.insert(snapshot);
        return snapshot;
    }

    @Override
    public void update(GatewayConfigSnapshot snapshot) {
        mapper.updateById(snapshot);
    }
}
