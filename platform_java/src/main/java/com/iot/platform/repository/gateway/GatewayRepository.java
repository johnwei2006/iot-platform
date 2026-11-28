package com.iot.platform.repository.gateway;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.Gateway;

import java.util.List;
import java.util.Optional;

/** 设备数据访问接口（MyBatis-Plus 适配器实现）。 */
public interface GatewayRepository {

    Optional<Gateway> findByDeviceId(String deviceId);

    List<Gateway> findByStatus(String status);

    List<Gateway> findAll();

    /** 按数据权限条件查询（null 或空 wrapper 表示不过滤）。 */
    List<Gateway> findAll(Wrapper<Gateway> wrapper);

    Optional<Gateway> findById(String id);

    Gateway save(Gateway device);

    void deleteById(String id);
}
