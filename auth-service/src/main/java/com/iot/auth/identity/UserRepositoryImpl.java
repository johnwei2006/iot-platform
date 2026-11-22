package com.iot.auth.identity;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.auth.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper mapper;

    public UserRepositoryImpl(UserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<User> findAll() {
        return mapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public User save(User user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(user.getId()) == null) {
            mapper.insert(user);
        } else {
            mapper.updateById(user);
        }
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)));
    }

    @Override
    public Optional<User> findByOpenid(String openid) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getOpenid, openid)));
    }

    @Override
    public Optional<User> findByUnionid(String unionid) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUnionid, unionid)));
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone)));
    }
}
