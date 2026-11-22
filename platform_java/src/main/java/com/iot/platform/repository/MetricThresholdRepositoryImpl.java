package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.MetricThresholdMapper;
import com.iot.platform.model.MetricThreshold;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MetricThresholdRepositoryImpl implements MetricThresholdRepository {

    private final MetricThresholdMapper mapper;

    public MetricThresholdRepositoryImpl(MetricThresholdMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<MetricThreshold> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<MetricThreshold> findAll(Wrapper<MetricThreshold> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public MetricThreshold save(MetricThreshold entity) {
        if (entity.getId() == null || entity.getId().isEmpty()) {
            entity.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(entity.getId()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return entity;
    }

    @Override
    public void deleteById(String id) {
        mapper.deleteById(id);
    }
}