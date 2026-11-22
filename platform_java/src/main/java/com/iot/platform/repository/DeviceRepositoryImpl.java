package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.DeviceMapper;
import com.iot.platform.model.Device;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceRepositoryImpl implements DeviceRepository {

    private final DeviceMapper mapper;

    public DeviceRepositoryImpl(DeviceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Device> findByDeviceId(String deviceId) {
        return Optional.ofNullable(mapper.selectOne(
                Wrappers.<Device>lambdaQuery().eq(Device::getDeviceId, deviceId)));
    }

    @Override
    public List<Device> findByStatus(String status) {
        return mapper.selectList(Wrappers.<Device>lambdaQuery().eq(Device::getStatus, status));
    }

    @Override
    public List<Device> findAll() {
        return mapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public List<Device> findAll(Wrapper<Device> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public Optional<Device> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Device save(Device device) {
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
        mapper.deleteById(id);
    }
}
