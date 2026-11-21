package com.proxy.persistence.mongodb;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proxy.persistence.mongodb.model.GatewayReconnectRecord;

@Repository
public interface GatewayReconnectRecordRepository extends MongoRepository<GatewayReconnectRecord, String> {

	List<GatewayReconnectRecord> findByDeviceIdOrderByConnectTimeDesc(String deviceId, Pageable pageable);

	long countByDeviceId(String deviceId);
}
