package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.iot.platform.model.Message;

import java.util.List;
import java.util.Optional;

public interface MessageRepository {

    Optional<Message> findById(String id);

    List<Message> findAll(Wrapper<Message> wrapper);

    long count(Wrapper<Message> wrapper);

    Message save(Message entity);

    void deleteById(String id);
}