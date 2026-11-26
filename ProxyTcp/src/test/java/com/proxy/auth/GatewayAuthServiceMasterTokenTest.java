package com.proxy.auth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.proxy.persistence.GatewayTokenStoreService;

/** 测试用万能 token 行为：绕过会话/IP 校验、下行填充、关闭后严格校验。 */
public class GatewayAuthServiceMasterTokenTest {

	private static final byte[] MASTER = "MASTER0001".getBytes(StandardCharsets.US_ASCII);

	private GatewayAuthService service;

	@BeforeEach
	public void setUp() throws Exception {
		service = new GatewayAuthService();
		setField("masterToken", MASTER);
		setField("gatewayTokenStoreService", new GatewayTokenStoreService() {
			@Override public byte[] issueToken(String deviceId) { return null; }
			@Override public byte[] getActiveToken(String deviceId) { return null; }
			@Override public void invalidateTokens(String deviceId) { }
		});
	}

	private void setField(String name, Object value) throws Exception {
		Field field = GatewayAuthService.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(service, value);
	}

	@Test
	public void testMasterTokenAcceptedWithoutSession() {
		assertTrue(service.validateToken("NOT_REGISTERED", MASTER, "10.0.0.1", 50001));
		assertTrue(service.isMasterToken(MASTER));
	}

	@Test
	public void testOtherTokenRejectedWithoutSession() {
		byte[] other = "ABCDEFGHIJ".getBytes(StandardCharsets.US_ASCII);
		assertFalse(service.isMasterToken(other));
		assertFalse(service.validateToken("NOT_REGISTERED", other, "10.0.0.1", 50001));
	}

	@Test
	public void testAllZeroTokenRejected() {
		assertFalse(service.validateToken("NOT_REGISTERED", new byte[10], "10.0.0.1", 50001));
	}

	@Test
	public void testDownlinkFallsBackToMasterToken() {
		assertArrayEquals(MASTER, service.getTokenForDownlink("NOT_REGISTERED"));
	}

	@Test
	public void testDisabledMasterToken() throws Exception {
		setField("masterToken", null);
		assertNull(service.getTokenForDownlink("NOT_REGISTERED"));
		assertFalse(service.isMasterToken(MASTER));
		assertFalse(service.validateToken("NOT_REGISTERED", MASTER, "10.0.0.1", 50001));
	}

	@Test
	public void testTokenCheckDisabledAcceptsAnyToken() throws Exception {
		setField("masterToken", null);
		setField("tokenCheckEnabled", false);
		assertFalse(service.isTokenCheckEnabled());
		assertTrue(service.validateToken("NOT_REGISTERED", new byte[10], "10.0.0.1", 50001));
		assertTrue(service.validateToken("NOT_REGISTERED", "ABCDEFGHIJ".getBytes(StandardCharsets.US_ASCII),
				"10.0.0.1", 50001));
	}
}
