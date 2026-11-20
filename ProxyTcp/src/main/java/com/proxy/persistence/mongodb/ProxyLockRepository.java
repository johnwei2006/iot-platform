package com.proxy.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proxy.persistence.mongodb.model.ProxyLock;

@Repository
public interface ProxyLockRepository extends MongoRepository<ProxyLock, String>, ProxyLockRepositoryCustom {
}
