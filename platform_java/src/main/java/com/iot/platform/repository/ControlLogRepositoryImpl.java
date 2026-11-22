package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.mapper.ControlLogMapper;
import com.iot.platform.model.ControlLog;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ControlLogRepositoryImpl implements ControlLogRepository {

    private final ControlLogMapper mapper;

    public ControlLogRepositoryImpl(ControlLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ControlLog> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<ControlLog> findAll(Wrapper<ControlLog> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public long count(Wrapper<ControlLog> wrapper) {
        Long c = mapper.selectCount(wrapper);
        return c == null ? 0L : c;
    }

    @Override
    public ControlLog save(ControlLog log) {
        if (log.getId() == null || log.getId().isEmpty()) {
            log.setId(UUID.randomUUID().toString());
        }
        if (log.getCreatedAt() == null) {
            log.setCreatedAt(new java.util.Date());
        }
        if (mapper.selectById(log.getId()) == null) {
            mapper.insert(log);
        } else {
            mapper.updateById(log);
        }
        return log;
    }

    @Override
    public void deleteById(String id) {
        mapper.deleteById(id);
    }
}
