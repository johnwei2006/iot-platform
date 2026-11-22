package com.iot.admin.menu;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 菜单/按钮数据访问接口（由 MyBatis-Plus 适配器实现，方法签名保持不变）。 */
public interface MenuRepository {

    Optional<Menu> findById(String id);

    List<Menu> findAll();

    Menu save(Menu menu);

    void delete(Menu menu);

    Optional<Menu> findByPath(String path);

    Optional<Menu> findByPerm(String perm);

    List<Menu> findByParentId(String parentId);

    List<Menu> findByIdIn(Collection<String> ids);
}
