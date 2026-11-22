package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    Optional<Task> findById(String id);

    List<Task> findAll(Wrapper<Task> wrapper);

    Task save(Task entity);

    void deleteById(String id);
}