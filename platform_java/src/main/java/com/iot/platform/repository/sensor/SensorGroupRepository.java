package com.iot.platform.repository.sensor;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.SensorGroup;

import java.util.List;
import java.util.Optional;

public interface SensorGroupRepository {
    List<SensorGroup> findAll();

    List<SensorGroup> findAll(Wrapper<SensorGroup> wrapper);

    Optional<SensorGroup> findById(String id);

    SensorGroup save(SensorGroup group);

    void deleteById(String id);
}
