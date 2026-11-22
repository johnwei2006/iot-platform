package com.iot.admin.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, String> {

    List<Menu> findByParentId(String parentId);

    List<Menu> findByIdIn(List<String> ids);

    List<Menu> findByStatus(String status);

    Optional<Menu> findByPerm(String perm);

    Optional<Menu> findByPath(String path);
}
