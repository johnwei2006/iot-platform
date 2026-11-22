package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.ControlRule;

import java.util.List;
import java.util.Optional;

public interface ControlRuleRepository {

    Optional<ControlRule> findById(String id);

    List<ControlRule> findAll();

    List<ControlRule> findAll(Wrapper<ControlRule> wrapper);

    Optional<ControlRule> findByName(String name);

    ControlRule save(ControlRule rule);

    void deleteById(String id);
}
