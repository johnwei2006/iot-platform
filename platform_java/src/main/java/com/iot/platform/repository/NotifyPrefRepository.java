package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.NotifyPref;

import java.util.List;
import java.util.Optional;

public interface NotifyPrefRepository {

    Optional<NotifyPref> findById(String id);

    List<NotifyPref> findAll(Wrapper<NotifyPref> wrapper);

    NotifyPref save(NotifyPref entity);

    void deleteById(String id);
}