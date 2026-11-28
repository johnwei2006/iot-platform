package com.iot.platform.repository.sensor;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.SensorGroupMapper;
import com.iot.platform.model.SensorGroup;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SensorGroupRepositoryImpl implements SensorGroupRepository {

    private final SensorGroupMapper mapper;

    public SensorGroupRepositoryImpl(SensorGroupMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<SensorGroup> findAll() {
        return mapper.selectList(Wrappers.<SensorGroup>lambdaQuery().eq(SensorGroup::getIsDeleted, 0));
    }

    @Override
    public List<SensorGroup> findAll(Wrapper<SensorGroup> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public Optional<SensorGroup> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public SensorGroup save(SensorGroup group) {
        if (group.getId() == null || group.getId().isEmpty()) {
            group.setId(UUID.randomUUID().toString());
        }
        if (group.getIsDeleted() == null) {
            group.setIsDeleted(0);
        }
        if (mapper.selectById(group.getId()) == null) {
            mapper.insert(group);
        } else {
            mapper.updateById(group);
        }
        return group;
    }

    @Override
    public void deleteById(String id) {
        mapper.update(null, new LambdaUpdateWrapper<SensorGroup>()
                .eq(SensorGroup::getId, id).set(SensorGroup::getIsDeleted, 1));
    }
}
