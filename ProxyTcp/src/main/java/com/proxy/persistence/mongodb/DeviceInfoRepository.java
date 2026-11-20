package com.proxy.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proxy.persistence.mongodb.model.DeviceInfo;

@Repository
public interface DeviceInfoRepository extends MongoRepository<DeviceInfo, String>, DeviceInfoRepositoryCustom {
}
