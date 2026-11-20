package com.iot.auth.identity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByOpenid(String openid);

    Optional<User> findByUnionid(String unionid);

    Optional<User> findByPhone(String phone);
}
