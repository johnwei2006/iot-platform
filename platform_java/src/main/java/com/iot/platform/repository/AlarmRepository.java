package com.iot.platform.repository;

import com.iot.platform.model.Alarm;

import java.util.List;
import java.util.Optional;

/** 告警数据访问接口（MyBatis-Plus 适配器实现）。 */
public interface AlarmRepository {

    List<Alarm> findByStatus(String status);

    List<Alarm> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    List<Alarm> findAll();

    Optional<Alarm> findById(String id);

    Alarm save(Alarm alarm);
}
