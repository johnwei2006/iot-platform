package com.iot.platform.repository.gateway;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.mapper.GatewayTaskMapper;
import com.iot.platform.model.GatewayTask;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class GatewayTaskRepositoryImpl implements GatewayTaskRepository {

    private final GatewayTaskMapper mapper;

    public GatewayTaskRepositoryImpl(GatewayTaskMapper mapper) { this.mapper = mapper; }

    @Override
    public List<GatewayTask> findAll(Wrapper<GatewayTask> wrapper) { return mapper.selectList(wrapper); }

    @Override
    public GatewayTask findById(String id) { return mapper.selectById(id); }

    @Override
    public GatewayTask save(GatewayTask task) {
        if (task.getId() == null || task.getId().isEmpty()) task.setId(UUID.randomUUID().toString());
        if (task.getCreatedAt() == null) task.setCreatedAt(new java.util.Date());
        task.setUpdatedAt(new java.util.Date());
        if (mapper.selectById(task.getId()) == null) mapper.insert(task); else mapper.updateById(task);
        return task;
    }

    @Override
    public void deleteById(String id) { mapper.deleteById(id); }
}
