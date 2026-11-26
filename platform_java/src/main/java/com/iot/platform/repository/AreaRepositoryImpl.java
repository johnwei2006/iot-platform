package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.AreaMapper;
import com.iot.platform.model.Area;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AreaRepositoryImpl implements AreaRepository {

    private final AreaMapper mapper;

    public AreaRepositoryImpl(AreaMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Area> findAll() {
        return mapper.selectList(Wrappers.<Area>lambdaQuery().eq(Area::getIsDeleted, 0));
    }

    @Override
    public List<Area> findAll(Wrapper<Area> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public Optional<Area> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Area save(Area area) {
        if (area.getId() == null || area.getId().isEmpty()) {
            area.setId(UUID.randomUUID().toString());
        }
        if (area.getIsDeleted() == null) {
            area.setIsDeleted(0);
        }
        if (mapper.selectById(area.getId()) == null) {
            mapper.insert(area);
        } else {
            mapper.updateById(area);
        }
        return area;
    }

    @Override
    public void deleteById(String id) {
        mapper.update(null, new LambdaUpdateWrapper<Area>()
                .eq(Area::getId, id).set(Area::getIsDeleted, 1));
    }
}
