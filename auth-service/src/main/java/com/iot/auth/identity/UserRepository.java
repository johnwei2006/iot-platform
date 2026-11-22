package com.iot.auth.identity;

import java.util.List;
import java.util.Optional;

/** 用户数据访问接口（由 MyBatis-Plus 适配器实现，方法签名保持不变）。 */
public interface UserRepository {

    List<User> findAll();

    Optional<User> findById(String id);

    User save(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findByOpenid(String openid);

    Optional<User> findByUnionid(String unionid);

    Optional<User> findByPhone(String phone);
}
