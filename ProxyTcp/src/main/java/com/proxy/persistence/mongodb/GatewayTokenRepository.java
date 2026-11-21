package com.proxy.persistence.mongodb;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.proxy.persistence.mongodb.model.GatewayToken;

@Repository
public interface GatewayTokenRepository extends MongoRepository<GatewayToken, String> {

	List<GatewayToken> findByDeviceId(String deviceId);

	GatewayToken findByDeviceIdAndActive(String deviceId, boolean active);
}
