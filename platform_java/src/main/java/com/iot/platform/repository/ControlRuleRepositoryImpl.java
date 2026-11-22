package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.ControlRuleMapper;
import com.iot.platform.model.ControlRule;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ControlRuleRepositoryImpl implements ControlRuleRepository {

    private final ControlRuleMapper mapper;

    public ControlRuleRepositoryImpl(ControlRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ControlRule> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<ControlRule> findAll() {
        return mapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public List<ControlRule> findAll(Wrapper<ControlRule> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public Optional<ControlRule> findByName(String name) {
        return Optional.ofNullable(mapper.selectOne(
                Wrappers.<ControlRule>lambdaQuery().eq(ControlRule::getName, name)));
    }

    @Override
    public ControlRule save(ControlRule rule) {
        if (rule.getId() == null || rule.getId().isEmpty()) {
            rule.setId(UUID.randomUUID().toString());
        }
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(new java.util.Date());
        }
        rule.setUpdatedAt(new java.util.Date());
        if (mapper.selectById(rule.getId()) == null) {
            mapper.insert(rule);
        } else {
            mapper.updateById(rule);
        }
        return rule;
    }

    @Override
    public void deleteById(String id) {
        mapper.deleteById(id);
    }
}
