package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.GatewayTask;

import java.util.List;

public interface GatewayTaskRepository {
    List<GatewayTask> findAll(Wrapper<GatewayTask> wrapper);
    GatewayTask findById(String id);
    GatewayTask save(GatewayTask task);
    void deleteById(String id);
}
