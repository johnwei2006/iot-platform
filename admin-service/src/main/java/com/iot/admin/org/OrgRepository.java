package com.iot.admin.org;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrgRepository extends JpaRepository<Org, String> {

    Optional<Org> findByCode(String code);

    List<Org> findByParentCode(String parentCode);

    List<Org> findByCodeStartingWith(String prefix);

    List<Org> findByStatus(String status);
}
