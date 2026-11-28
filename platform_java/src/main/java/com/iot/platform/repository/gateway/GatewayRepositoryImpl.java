package com.iot.platform.repository.gateway;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.GatewayMapper;
import com.iot.platform.model.Gateway;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class GatewayRepositoryImpl implements GatewayRepository {

    private final GatewayMapper mapper;

    public GatewayRepositoryImpl(GatewayMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Gateway> findByDeviceId(String deviceId) {
        return Optional.ofNullable(mapper.selectOne(
                Wrappers.<Gateway>lambdaQuery().eq(Gateway::getDeviceId, deviceId)));
    }

    @Override
    public List<Gateway> findByStatus(String status) {
        return mapper.selectList(Wrappers.<Gateway>lambdaQuery().eq(Gateway::getStatus, status));
    }

    @Override
    public List<Gateway> findAll() {
        return mapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public List<Gateway> findAll(Wrapper<Gateway> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public Optional<Gateway> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Gateway save(Gateway device) {
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
