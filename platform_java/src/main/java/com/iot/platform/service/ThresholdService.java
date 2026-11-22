package com.iot.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.MetricThreshold;
import com.iot.platform.repository.MetricThresholdRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/** 测点阈值：按设备+指标+测点唯一 upsert；供超限告警联动与双端阈值页查询。 */
@Service
public class ThresholdService {

    private final MetricThresholdRepository repository;

    public ThresholdService(MetricThresholdRepository repository) {
        this.repository = repository;
    }

    public List<MetricThreshold> listByDevice(String deviceId) {
        return repository.findAll(Wrappers.<MetricThreshold>lambdaQuery()
                .eq(MetricThreshold::getDeviceId, deviceId));
    }

    /** upsert：命中唯一键（device+metric+station）则更新上下限。 */
    public MetricThreshold upsert(MetricThreshold incoming) {
        if (incoming.getMetric() == null || incoming.getMetric().isEmpty()) {
            throw new IllegalArgumentException("请选择指标");
        }
        String station = incoming.getStation() == null ? "" : incoming.getStation();
        List<MetricThreshold> existing = repository.findAll(Wrappers.<MetricThreshold>lambdaQuery()
                .eq(MetricThreshold::getDeviceId, incoming.getDeviceId())
                .eq(MetricThreshold::getMetric, incoming.getMetric()));
        MetricThreshold target = existing.stream()
                .filter(t -> station.equals(t.getStation() == null ? "" : t.getStation()))
                .findFirst()
                .orElse(null);
        Date now = new Date();
        if (target == null) {
            incoming.setId(UUID.randomUUID().toString());
            incoming.setStation(station.isEmpty() ? null : station);
            incoming.setCreatedAt(now);
            incoming.setUpdatedAt(now);
            return repository.save(incoming);
        }
        target.setMinValue(incoming.getMinValue());
        target.setMaxValue(incoming.getMaxValue());
        target.setUpdatedBy(incoming.getUpdatedBy());
        target.setUpdatedAt(now);
        return repository.save(target);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    /** 超限判定：值低于下限或高于上限（阈值缺省视为不超限）。 */
    public static boolean exceeds(MetricThreshold t, double value) {
        if (t == null) {
            return false;
        }
        if (t.getMinValue() != null && value < t.getMinValue().doubleValue()) {
            return true;
        }
        return t.getMaxValue() != null && value > t.getMaxValue().doubleValue();
    }
}