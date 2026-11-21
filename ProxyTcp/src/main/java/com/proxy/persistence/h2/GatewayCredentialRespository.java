package com.proxy.persistence.h2;

import java.util.Date;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.proxy.persistence.h2.model.GatewayCredential;

@Repository
public interface GatewayCredentialRespository extends CrudRepository<GatewayCredential, Long> {

	GatewayCredential findByDeviceId(String deviceId);

	void deleteByDeviceId(String deviceId);
}
