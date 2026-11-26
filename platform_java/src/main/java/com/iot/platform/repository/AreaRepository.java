package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.Area;

import java.util.List;
import java.util.Optional;

public interface AreaRepository {
    List<Area> findAll();

    List<Area> findAll(Wrapper<Area> wrapper);

    Optional<Area> findById(String id);

    Area save(Area area);

    void deleteById(String id);
}
