package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.MessageMapper;
import com.iot.platform.model.Message;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MessageRepositoryImpl implements MessageRepository {

    private final MessageMapper mapper;

    public MessageRepositoryImpl(MessageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Message> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<Message> findAll(Wrapper<Message> wrapper) {
        return mapper.selectList(wrapper);
    }

    @Override
    public long count(Wrapper<Message> wrapper) {
        Long c = mapper.selectCount(wrapper);
        return c == null ? 0L : c;
    }

    @Override
    public Message save(Message entity) {
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