package com.iot.admin.role;

import java.util.List;
import java.util.Optional;

/** 角色数据访问接口（由 MyBatis-Plus 适配器实现，方法签名保持不变）。 */
public interface RoleRepository {

    Optional<Role> findById(String id);

    List<Role> findAll();

    Role save(Role role);

    void delete(Role role);

    Optional<Role> findByCode(String code);
}
