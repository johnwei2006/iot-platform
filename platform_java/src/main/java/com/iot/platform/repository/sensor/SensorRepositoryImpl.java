package com.iot.platform.repository.sensor;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.SensorMapper;
import com.iot.platform.model.Sensor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SensorRepositoryImpl implements SensorRepository {

    private final SensorMapper mapper;

    public SensorRepositoryImpl(SensorMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Sensor> findBySensorCode(String sensorCode) {
        return Optional.ofNullable(mapper.selectOne(
                Wrappers.<Sensor>lambdaQuery().eq(Sensor::getSensorCode, sensorCode)));
    }

    @Override
    public List<Sensor> findByNodeCode(String nodeCode) {
        return mapper.selectList(Wrappers.<Sensor>lambdaQuery()
                .eq(Sensor::getNodeCode, nodeCode).eq(Sensor::getIsDeleted, 0));
    }

    @Override
    public List<Sensor> findAll() {
        return mapper.selectList(Wrappers.<Sensor>lambdaQuery().eq(Sensor::getIsDeleted, 0));
    }

    @Override
    public List<Sensor> findAll(Wrapper<Sensor> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public Optional<Sensor> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Sensor save(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(sensor.getId()) == null) {
            mapper.insert(sensor);
        } else {
            mapper.updateById(sensor);
        }
        return sensor;
    }

    @Override
    public void deleteById(String id) {
        mapper.update(null, new LambdaUpdateWrapper<Sensor>()
                .eq(Sensor::getId, id).set(Sensor::getIsDeleted, 1));
    }
}
