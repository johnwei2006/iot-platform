package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.DeviceCtrlMapper;
import com.iot.platform.model.DeviceCtrl;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceCtrlRepositoryImpl implements DeviceCtrlRepository {

    private final DeviceCtrlMapper mapper;

    public DeviceCtrlRepositoryImpl(DeviceCtrlMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<DeviceCtrl> findByDeviceCode(String deviceCode) {
        return Optional.ofNullable(mapper.selectOne(
                Wrappers.<DeviceCtrl>lambdaQuery().eq(DeviceCtrl::getDeviceCode, deviceCode)));
    }

    @Override
    public List<DeviceCtrl> findByNodeCode(String nodeCode) {
        return mapper.selectList(Wrappers.<DeviceCtrl>lambdaQuery()
                .eq(DeviceCtrl::getNodeCode, nodeCode).eq(DeviceCtrl::getIsDeleted, 0));
    }

    @Override
    public List<DeviceCtrl> findAll() {
        return mapper.selectList(Wrappers.<DeviceCtrl>lambdaQuery().eq(DeviceCtrl::getIsDeleted, 0));
    }

    @Override
    public List<DeviceCtrl> findAll(Wrapper<DeviceCtrl> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public Optional<DeviceCtrl> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public DeviceCtrl save(DeviceCtrl device) {
        if (device.getId() == null || device.getId().isEmpty()) {
            device.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(device.getId()) == null) {
            mapper.insert(device);
        } else {
            mapper.updateById(device);
        }
        return device;
    }

    @Override
    public void deleteById(String id) {
        mapper.update(null, new LambdaUpdateWrapper<DeviceCtrl>()
                .eq(DeviceCtrl::getId, id).set(DeviceCtrl::getIsDeleted, 1));
    }
}
