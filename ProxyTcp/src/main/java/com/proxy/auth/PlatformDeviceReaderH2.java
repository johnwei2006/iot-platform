package com.proxy.auth;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.proxy.persistence.h2.GatewayCredentialRespository;
import com.proxy.persistence.h2.model.GatewayCredential;

/**
 * H2 模式网关凭据读取：读取本地 GatewayCredential 表。
 * 密钥通过 REST 运维接口 POST /api/gateway/credentials 录入。
 */
@Service
@Profile("h2")
public class PlatformDeviceReaderH2 implements PlatformDeviceReader {

	@Autowired
	private GatewayCredentialRespository gatewayCredentialRespository;

	@Override
	public String getAuthSecret(String deviceId) {
		GatewayCredential credential = gatewayCredentialRespository.findByDeviceId(deviceId);
		return credential == null ? null : credential.getAuthSecret();
	}

	@Override
	public boolean upsertAuthSecret(String deviceId, String secret) {
		GatewayCredential credential = gatewayCredentialRespository.findByDeviceId(deviceId);
		if (credential == null) {
			credential = new GatewayCredential();
			credential.setDeviceId(deviceId);
		}
		credential.setAuthSecret(secret);
		credential.setUpdatedAt(new Date());
		gatewayCredentialRespository.save(credential);
		return true;
	}
}
