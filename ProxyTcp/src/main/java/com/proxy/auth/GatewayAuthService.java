package com.proxy.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proxy.persistence.DeviceInfoService;
import com.proxy.persistence.GatewayRecordService;
import com.proxy.persistence.GatewayTokenStoreService;
import com.proxy.persistence.model.DeviceInfo;
import com.proxy.utils.HexConvert;

/**
 * 网关认证核心服务：校验 gateway_id + 预共享密钥、签发/验证 10 字节 token、
 * 维护连接会话（连接断开即失效，重连必须重新认证）、记录重连与 token 申请记录。
 */
@Service
public class GatewayAuthService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private PlatformDeviceReader platformDeviceReader;

	@Autowired
	private GatewayTokenStoreService gatewayTokenStoreService;

	@Autowired
	private GatewayRecordService gatewayRecordService;

	@Autowired
	private DeviceInfoService deviceInfoService;

	/** deviceId -> 已认证会话（ip:port） */
	private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();

	/** "ip:port" -> deviceId，用于断开时定位设备与连接事件去重 */
	private final ConcurrentHashMap<String, String> connectionDevices = new ConcurrentHashMap<String, String>();

	/** 已认证会话 */
	public static class Session {
		public String ip;
		public int port;

		Session(String ip, int port) {
			this.ip = ip;
			this.port = port;
		}
	}

	/** 认证结果 */
	public static class AuthResult {
		public boolean success;
		public int code;
		public byte[] token;
	}

	/**
	 * 处理网关认证请求：校验已入库网关的预共享密钥，成功则签发新 token（旧 token 作废）。
	 */
	public AuthResult handleAuthRequest(String deviceId, byte[] serviceData, String ip, int port) {
		AuthResult result = new AuthResult();
		result.success = false;
		String resultStr;
		try {
			String secret = extractSecret(serviceData);
			String storedSecret = platformDeviceReader.getAuthSecret(deviceId);
			if (storedSecret == null || storedSecret.isEmpty()) {
				result.code = GatewayAuthConstants.AUTH_RESULT_UNKNOWN_DEVICE;
				resultStr = "UNKNOWN_DEVICE";
				logger.warn("Gateway auth failed, device not registered: deviceId=" + deviceId);
			} else if (secret == null || !constantTimeEquals(secret, storedSecret.trim())) {
				result.code = GatewayAuthConstants.AUTH_RESULT_INVALID_SECRET;
				resultStr = "INVALID_SECRET";
				logger.warn("Gateway auth failed, invalid secret: deviceId=" + deviceId + ", ip=" + ip + ":" + port);
			} else {
				byte[] token = gatewayTokenStoreService.issueToken(deviceId);
				result.success = true;
				result.code = GatewayAuthConstants.AUTH_RESULT_SUCCESS;
				result.token = token;
				resultStr = "SUCCESS";
				sessions.put(deviceId, new Session(ip, port));
				connectionDevices.put(ip + ":" + port, deviceId);
				logger.info("Gateway auth success: deviceId=" + deviceId + ", ip=" + ip + ":" + port
						+ ", token=" + HexConvert.toHexString(token));
			}
		} catch (Exception e) {
			result.code = GatewayAuthConstants.AUTH_RESULT_ERROR;
			resultStr = "ERROR";
			logger.error("Gateway auth error: deviceId=" + deviceId + ", error=" + e.getMessage(), e);
		}
		try {
			gatewayRecordService.saveTokenRequestRecord(deviceId, ip, port, resultStr,
					result.token == null ? null : HexConvert.toHexString(result.token));
		} catch (Exception e) {
			logger.error("Failed to save token request record: deviceId=" + deviceId + ", error=" + e.getMessage());
		}
		return result;
	}

	/**
	 * 记录连接事件：同一 TCP 连接只记录一次；设备此前已存在会话/在线记录则记为 RECONNECT，否则 FIRST。
	 */
	public void recordConnection(String deviceId, String ip, int port) {
		String connKey = ip + ":" + port;
		String previous = connectionDevices.putIfAbsent(connKey, deviceId);
		if (previous != null) {
			return;
		}
		try {
			String type;
			if (sessions.containsKey(deviceId)) {
				type = GatewayAuthConstants.CONNECT_TYPE_RECONNECT;
			} else {
				DeviceInfo deviceInfo = deviceInfoService.queryDeviceInfo(deviceId);
				type = deviceInfo == null ? GatewayAuthConstants.CONNECT_TYPE_FIRST
						: GatewayAuthConstants.CONNECT_TYPE_RECONNECT;
			}
			gatewayRecordService.saveReconnectRecord(deviceId, ip, port, type);
			logger.info("Gateway " + type + " connection: deviceId=" + deviceId + ", ip=" + ip + ":" + port);
		} catch (Exception e) {
			logger.error("Failed to save reconnect record: deviceId=" + deviceId + ", error=" + e.getMessage());
		}
	}

	/**
	 * 校验上行报文 token：会话存在、连接匹配且 token 与当前有效 token 一致。
	 */
	public boolean validateToken(String deviceId, byte[] token, String ip, int port) {
		if (token == null || isAllZero(token)) {
			return false;
		}
		Session session = sessions.get(deviceId);
		if (session == null) {
			return false;
		}
		if (!session.ip.equals(ip) || session.port != port) {
			return false;
		}
		byte[] active = gatewayTokenStoreService.getActiveToken(deviceId);
		if (active == null) {
			return false;
		}
		return MessageDigest.isEqual(active, token);
	}

	/**
	 * TCP 连接断开：清除会话并作废该网关 token（重连后必须重新认证）。
	 */
	public void onChannelClosed(String ip, int port) {
		String deviceId = connectionDevices.remove(ip + ":" + port);
		if (deviceId == null) {
			return;
		}
		Session session = sessions.get(deviceId);
		if (session != null && session.ip.equals(ip) && session.port == port) {
			sessions.remove(deviceId);
		}
		try {
			gatewayTokenStoreService.invalidateTokens(deviceId);
			logger.info("Gateway connection closed, session removed and token invalidated: deviceId=" + deviceId);
		} catch (Exception e) {
			logger.error("Failed to invalidate tokens for deviceId=" + deviceId + ", error=" + e.getMessage());
		}
	}

	/** 该网关当前是否已认证（存在有效会话） */
	public boolean hasActiveSession(String deviceId) {
		return sessions.containsKey(deviceId);
	}

	/**
	 * 获取网关当前有效 token（下行报文填充用）。
	 * @return token 字节；未认证返回 null（编码时填全 0）
	 */
	public byte[] getTokenForDownlink(String deviceId) {
		return gatewayTokenStoreService.getActiveToken(deviceId);
	}

	private String extractSecret(byte[] serviceData) {
		if (serviceData == null || serviceData.length == 0) {
			return null;
		}
		int end = Math.min(serviceData.length, GatewayAuthConstants.AUTH_SECRET_MAX_LENGTH);
		while (end > 0 && serviceData[end - 1] == 0) {
			end--;
		}
		return new String(serviceData, 0, end, StandardCharsets.US_ASCII).trim();
	}

	private boolean isAllZero(byte[] bytes) {
		for (byte b : bytes) {
			if (b != 0) {
				return false;
			}
		}
		return true;
	}

	/** 常量时间字符串比较，防时序攻击 */
	private boolean constantTimeEquals(String a, String b) {
		return MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII), b.getBytes(StandardCharsets.US_ASCII));
	}
}
