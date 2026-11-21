package com.proxy.auth;

/**
 * 平台网关凭据读取接口：认证时校验"已入库"网关的预共享密钥。
 * Mongo 模式读 platform_java 的 iot_platform.devices 集合；
 * H2 模式读本地 gateway_credential 表（通过 REST 运维接口录入）。
 */
public interface PlatformDeviceReader {

	/**
	 * 查询网关入库的预共享密钥。
	 * @return 密钥；网关未入库返回 null
	 */
	String getAuthSecret(String deviceId);

	/**
	 * 写入/更新网关预共享密钥（运维接口）。
	 * @return 是否成功
	 */
	boolean upsertAuthSecret(String deviceId, String secret);
}
