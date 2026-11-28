package com.iot.platform.repository.device;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.DeviceCtrl;

import java.util.List;
import java.util.Optional;

/** 控制设备数据访问接口。 */
public interface DeviceCtrlRepository {

    Optional<DeviceCtrl> findByDeviceCode(String deviceCode);

    List<DeviceCtrl> findByNodeCode(String nodeCode);

    List<DeviceCtrl> findAll();

    List<DeviceCtrl> findAll(Wrapper<DeviceCtrl> wrapper);

    Optional<DeviceCtrl> findById(String id);

    DeviceCtrl save(DeviceCtrl device);

    void deleteById(String id);
}
