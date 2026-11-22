package com.iot.admin.org;

import java.util.List;
import java.util.Optional;

/** 机构数据访问接口（由 MyBatis-Plus 适配器实现，方法签名保持不变）。 */
public interface OrgRepository {

    Optional<Org> findById(String id);

    List<Org> findAll();

    Org save(Org org);

    void delete(Org org);

    Optional<Org> findByCode(String code);

    List<Org> findByParentCode(String parentCode);

    List<Org> findByCodeStartingWith(String code);
}
