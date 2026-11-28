package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.mapper.GatewayInfoHistoryMapper;
import com.iot.platform.model.GatewayInfoHistory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class GatewayInfoHistoryRepositoryImpl implements GatewayInfoHistoryRepository {

    private final GatewayInfoHistoryMapper mapper;

    public GatewayInfoHistoryRepositoryImpl(GatewayInfoHistoryMapper mapper) { this.mapper = mapper; }

    @Override
    public List<GatewayInfoHistory> findAll(Wrapper<GatewayInfoHistory> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public IPage<GatewayInfoHistory> findPage(Page<GatewayInfoHistory> page, Wrapper<GatewayInfoHistory> wrapper) {
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public GatewayInfoHistory save(GatewayInfoHistory record) {
        if (record.getId() == null || record.getId().isEmpty()) {
            record.setId(UUID.randomUUID().toString());
        }
        mapper.insert(record);
        return record;
    }
}
