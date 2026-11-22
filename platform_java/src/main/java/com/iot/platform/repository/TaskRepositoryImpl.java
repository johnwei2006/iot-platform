package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.TaskMapper;
import com.iot.platform.model.Task;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskMapper mapper;

    public TaskRepositoryImpl(TaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Task> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<Task> findAll(Wrapper<Task> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public Task save(Task entity) {
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