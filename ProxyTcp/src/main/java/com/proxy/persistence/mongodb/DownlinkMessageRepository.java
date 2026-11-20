package com.proxy.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proxy.persistence.mongodb.model.DownlinkMessage;

@Repository
public interface DownlinkMessageRepository extends MongoRepository<DownlinkMessage, String>, DownlinkMessageRepositoryCustom{
}
