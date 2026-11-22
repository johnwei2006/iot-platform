package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.ControlLog;

import java.util.List;
import java.util.Optional;

public interface ControlLogRepository {

    Optional<ControlLog> findById(String id);

    List<ControlLog> findAll(Wrapper<ControlLog> wrapper);

    long count(Wrapper<ControlLog> wrapper);

    ControlLog save(ControlLog log);

    void deleteById(String id);
}
