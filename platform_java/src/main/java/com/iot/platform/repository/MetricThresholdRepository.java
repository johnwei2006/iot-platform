package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.MetricThreshold;

import java.util.List;
import java.util.Optional;

public interface MetricThresholdRepository {

    Optional<MetricThreshold> findById(String id);

    List<MetricThreshold> findAll(Wrapper<MetricThreshold> wrapper);

    MetricThreshold save(MetricThreshold entity);

    void deleteById(String id);
}