package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.Sensor;

import java.util.List;
import java.util.Optional;

/** 传感器数据访问接口。 */
public interface SensorRepository {

    Optional<Sensor> findBySensorCode(String sensorCode);

    List<Sensor> findByNodeCode(String nodeCode);

    List<Sensor> findAll();

    List<Sensor> findAll(Wrapper<Sensor> wrapper);

    Optional<Sensor> findById(String id);

    Sensor save(Sensor sensor);

    void deleteById(String id);
}
