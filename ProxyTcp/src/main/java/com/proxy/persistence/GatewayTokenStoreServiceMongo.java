package com.proxy.persistence;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.proxy.model.CommonTransportMessage;
import com.proxy.persistence.mongodb.GatewayTokenRepository;
import com.proxy.persistence.mongodb.model.GatewayToken;
import com.proxy.utils.HexConvert;

/**
 * Mongo 模式网关认证 token 存储。token 以 hex 存储于 proxy 库 gateway_token 集合。
 */
@Service
@org.springframework.context.annotation.Profile("mongo")
public class GatewayTokenStoreServiceMongo implements GatewayTokenStoreService {

	@Autowired
	private GatewayTokenRepository gatewayTokenRepository;

	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public byte[] issueToken(String deviceId) {
		byte[] token = new byte[CommonTransportMessage.TOKEN_LENGTH];
		secureRandom.nextBytes(token);

		// 作废旧 token
		List<GatewayToken> oldTokens = gatewayTokenRepository.findByDeviceId(deviceId);
		for (GatewayToken old : oldTokens) {
			if (old.isActive()) {
				old.setActive(false);
				gatewayTokenRepository.save(old);
			}
		}

		GatewayToken gatewayToken = new GatewayToken();
		gatewayToken.setDeviceId(deviceId);
		gatewayToken.setTokenHex(HexConvert.toHexString(token));
		gatewayToken.setIssuedAt(new Date());
		gatewayToken.setActive(true);
		gatewayTokenRepository.save(gatewayToken);
		return token;
	}

	@Override
	public byte[] getActiveToken(String deviceId) {
		GatewayToken token = gatewayTokenRepository.findByDeviceIdAndActive(deviceId, true);
		if (token == null || token.getTokenHex() == null) {
			return null;
		}
		return HexConvert.fromHexString(token.getTokenHex());
	}

	@Override
	public void invalidateTokens(String deviceId) {
		List<GatewayToken> tokens = gatewayTokenRepository.findByDeviceId(deviceId);
		for (GatewayToken token : tokens) {
			if (token.isActive()) {
				token.setActive(false);
				gatewayTokenRepository.save(token);
			}
		}
	}
}
