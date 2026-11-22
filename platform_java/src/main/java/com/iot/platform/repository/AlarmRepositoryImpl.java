package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.AlarmMapper;
import com.iot.platform.model.Alarm;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AlarmRepositoryImpl implements AlarmRepository {

    private final AlarmMapper mapper;

    public AlarmRepositoryImpl(AlarmMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Alarm> findByStatus(String status) {
        return mapper.selectList(Wrappers.<Alarm>lambdaQuery().eq(Alarm::getStatus, status));
    }

    @Override
    public List<Alarm> findByDeviceIdOrderByCreatedAtDesc(String deviceId) {
        return mapper.selectList(Wrappers.<Alarm>lambdaQuery()
                .eq(Alarm::getDeviceId, deviceId)
                .orderByDesc(Alarm::getCreatedAt));
    }

    @Override
    public List<Alarm> findAll() {
        return mapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public Optional<Alarm> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Alarm save(Alarm alarm) {
        if (alarm.getId() == null || alarm.getId().isEmpty()) {
            alarm.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(alarm.getId()) == null) {
            mapper.insert(alarm);
        } else {
            mapper.updateById(alarm);
        }
        return alarm;
    }
}
