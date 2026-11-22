package com.iot.admin.org;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrgRepository extends MongoRepository<Org, String> {

    Optional<Org> findByCode(String code);

    List<Org> findByParentCode(String parentCode);

    List<Org> findByCodeStartingWith(String prefix);

    List<Org> findByStatus(String status);
}
