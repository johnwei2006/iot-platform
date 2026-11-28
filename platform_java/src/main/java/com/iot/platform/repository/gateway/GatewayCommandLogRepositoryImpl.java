package com.iot.platform.repository.gateway;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.GatewayCommandLogMapper;
import com.iot.platform.model.GatewayCommandLog;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class GatewayCommandLogRepositoryImpl implements GatewayCommandLogRepository {

    private final GatewayCommandLogMapper mapper;

    public GatewayCommandLogRepositoryImpl(GatewayCommandLogMapper mapper) { this.mapper = mapper; }

    @Override
    public List<GatewayCommandLog> findAll(Wrapper<GatewayCommandLog> wrapper) { return mapper.selectList(wrapper); }

    @Override
    public GatewayCommandLog findByRequestId(String requestId) {
        if (requestId == null || requestId.isEmpty()) return null;
        return mapper.selectOne(Wrappers.<GatewayCommandLog>lambdaQuery()
                .eq(GatewayCommandLog::getRequestId, requestId));
    }

    @Override
    public GatewayCommandLog save(GatewayCommandLog log) {
        if (log.getId() == null || log.getId().isEmpty()) log.setId(UUID.randomUUID().toString());
        if (log.getCreatedAt() == null) log.setCreatedAt(new java.util.Date());
        if (mapper.selectById(log.getId()) == null) mapper.insert(log); else mapper.updateById(log);
        return log;
    }
}
