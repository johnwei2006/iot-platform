package com.proxy.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proxy.persistence.mongodb.model.UplinkMessage;

@Repository
public interface UplinkMessageRepository extends MongoRepository<UplinkMessage, String>, UplinkMessageRepositoryCustom {
}
