package com.proxy.persistence.h2;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.proxy.persistence.h2.model.GatewayToken;

@Repository
public interface GatewayTokenRespository extends CrudRepository<GatewayToken, Long> {

	List<GatewayToken> findByDeviceId(String deviceId);

	GatewayToken findByDeviceIdAndActiveTrue(String deviceId);
}
