package com.iot.auth.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByOpenid(String openid);

    Optional<User> findByUnionid(String unionid);

    Optional<User> findByPhone(String phone);
}
