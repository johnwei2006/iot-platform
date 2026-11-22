package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.Device;

import java.util.List;
import java.util.Optional;

/** 设备数据访问接口（MyBatis-Plus 适配器实现）。 */
public interface DeviceRepository {

    Optional<Device> findByDeviceId(String deviceId);

    List<Device> findByStatus(String status);

    List<Device> findAll();

    /** 按数据权限条件查询（null 或空 wrapper 表示不过滤）。 */
    List<Device> findAll(Wrapper<Device> wrapper);

    Optional<Device> findById(String id);

    Device save(Device device);

    void deleteById(String id);
}
