package com.proxy.persistence;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.proxy.model.CommonTransportMessage;
import com.proxy.persistence.h2.GatewayTokenRespository;
import com.proxy.persistence.h2.model.GatewayToken;
import com.proxy.utils.HexConvert;

/**
 * H2 模式网关认证 token 存储。
 */
@Service
@org.springframework.context.annotation.Profile("h2")
public class GatewayTokenStoreServiceH2 implements GatewayTokenStoreService {

	@Autowired
	private GatewayTokenRespository gatewayTokenRespository;

	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public byte[] issueToken(String deviceId) {
		byte[] token = new byte[CommonTransportMessage.TOKEN_LENGTH];
		secureRandom.nextBytes(token);

		List<GatewayToken> oldTokens = gatewayTokenRespository.findByDeviceId(deviceId);
		for (GatewayToken old : oldTokens) {
			if (old.isActive()) {
				old.setActive(false);
				gatewayTokenRespository.save(old);
			}
		}

		GatewayToken gatewayToken = new GatewayToken();
		gatewayToken.setDeviceId(deviceId);
		gatewayToken.setTokenHex(HexConvert.toHexString(token));
		gatewayToken.setIssuedAt(new Date());
		gatewayToken.setActive(true);
		gatewayTokenRespository.save(gatewayToken);
		return token;
	}

	@Override
	public byte[] getActiveToken(String deviceId) {
		GatewayToken token = gatewayTokenRespository.findByDeviceIdAndActiveTrue(deviceId);
		if (token == null || token.getTokenHex() == null) {
			return null;
		}
		return HexConvert.fromHexString(token.getTokenHex());
	}

	@Override
	public void invalidateTokens(String deviceId) {
		List<GatewayToken> tokens = gatewayTokenRespository.findByDeviceId(deviceId);
		for (GatewayToken token : tokens) {
			if (token.isActive()) {
				token.setActive(false);
				gatewayTokenRespository.save(token);
			}
		}
	}
}
