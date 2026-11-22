package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.NotifyPrefMapper;
import com.iot.platform.model.NotifyPref;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotifyPrefRepositoryImpl implements NotifyPrefRepository {

    private final NotifyPrefMapper mapper;

    public NotifyPrefRepositoryImpl(NotifyPrefMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<NotifyPref> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<NotifyPref> findAll(Wrapper<NotifyPref> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public NotifyPref save(NotifyPref entity) {
        if (entity.getId() == null || entity.getId().isEmpty()) {
            entity.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(entity.getId()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return entity;
    }

    @Override
    public void deleteById(String id) {
        mapper.deleteById(id);
    }
}