package com.proxy.persistence;

/**
 * 网关认证 token 存储接口。每个网关同一时刻只有一条有效 token；
 * 每次重连重新签发时旧 token 立即作废。
 */
public interface GatewayTokenStoreService {

	/**
	 * 作废该网关现有 token 并签发新 token（10 字节随机）。
	 * @return 新 token 字节
	 */
	byte[] issueToken(String deviceId);

	/**
	 * 查询网关当前有效 token。
	 * @return token 字节；无有效 token 返回 null
	 */
	byte[] getActiveToken(String deviceId);

	/** 作废该网关所有 token（连接断开时调用） */
	void invalidateTokens(String deviceId);
}
