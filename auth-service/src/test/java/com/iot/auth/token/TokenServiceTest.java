package com.iot.auth.token;

import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TokenServiceTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthProperties properties;
    private TokenService tokenService;

    @BeforeEach
    public void setUp() {
        properties = new AuthProperties();
        properties.setJwtSecret("test-secret-at-least-32-characters-long-1234567890");
        properties.setRedisPrefix("auth:");
        when(redis.opsForValue()).thenReturn(valueOperations);
        tokenService = new TokenService(properties, redis);
        tokenService.initCrypto();
    }

    @Test
    public void accessTokenRoundTrip() {
        TokenClaims claims = new TokenClaims("u1", "web", Arrays.asList("ADMIN"),
                "jti-1", 3L, "device-1");

        String token = tokenService.issueAccessToken(claims);
        TokenClaims parsed = tokenService.verifyAccessToken(token);

        assertEquals("u1", parsed.getSubject());
        assertEquals("web", parsed.getClientId());
        assertEquals(Arrays.asList("ADMIN"), parsed.getRoles());
        assertEquals("jti-1", parsed.getJti());
        assertEquals(3L, parsed.getTokenVersion());
        assertEquals("device-1", parsed.getDeviceId());
    }

    @Test
    public void accessTokenCarriesOrgAndDataScopes() {
        java.util.Map<String, String> ds = new java.util.LinkedHashMap<String, String>();
        ds.put("device", "ORG_AND_CHILDREN");
        ds.put("alarm", "SELF");
        TokenClaims claims = new TokenClaims("u1", "web", Arrays.asList("ADMIN"),
                "jti-org", 1L, "", "100102", ds);

        TokenClaims parsed = tokenService.verifyAccessToken(tokenService.issueAccessToken(claims));

        assertEquals("100102", parsed.getOrgCode());
        assertEquals("ORG_AND_CHILDREN", parsed.getDataScopes().get("device"));
        assertEquals("SELF", parsed.getDataScopes().get("alarm"));
        assertEquals(2, parsed.getDataScopes().size());
    }

    @Test
    public void invalidTokenIsRejected() {
        try {
            tokenService.verifyAccessToken("not-a-jwt");
            fail("invalid token should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void missingTokenIsRejected() {
        try {
            tokenService.verifyAccessToken(null);
            fail("missing token should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void refreshTokenIsStoredInRedisWithTtl() {
        String refresh = tokenService.issueRefreshToken("u1", "web", "d1", 5L);

        assertNotNull(refresh);
        verify(valueOperations).set(eq("auth:refresh:" + refresh),
                eq("u1|web|d1|5"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void rotationConsumesTokenAndWritesTombstone() {
        when(valueOperations.get("auth:refresh:abc")).thenReturn("u2|mini|device-2|7");

        TokenService.RefreshData data = tokenService.rotateRefresh("abc");

        assertEquals("u2", data.getUserId());
        assertEquals("mini", data.getClientId());
        assertEquals("device-2", data.getDeviceId());
        assertEquals(7L, data.getVersion());
        verify(redis).delete("auth:refresh:abc");
        verify(valueOperations).set(eq("auth:refresh-used:abc"), eq("u2"),
                anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void missingRefreshIsRejected() {
        when(valueOperations.get("auth:refresh:missing")).thenReturn(null);
        when(valueOperations.get("auth:refresh-used:missing")).thenReturn(null);
        try {
            tokenService.rotateRefresh("missing");
            fail("missing refresh should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void reusedRefreshIsDetectedViaTombstone() {
        when(valueOperations.get("auth:refresh:r1")).thenReturn(null);
        when(valueOperations.get("auth:refresh-used:r1")).thenReturn("u9");

        try {
            tokenService.rotateRefresh("r1");
            fail("rotated refresh reuse should be detected");
        } catch (TokenService.RefreshReuseException e) {
            assertEquals("u9", e.getUserId());
        }
    }

    @Test
    public void missingRefreshArgumentIsRejected() {
        try {
            tokenService.rotateRefresh(" ");
            fail("blank refresh should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void issuedAccessFromRefreshCarriesUserRolesAndVersion() {
        TokenService.RefreshData data = new TokenService.RefreshData("u3", "app", "d3", 9L);

        String token = tokenService.issueAccessFromRefresh(data, Arrays.asList("OPERATOR"),
                "100101", java.util.Collections.<String, String>emptyMap());
        TokenClaims parsed = tokenService.verifyAccessToken(token);

        assertEquals("u3", parsed.getSubject());
        assertEquals("app", parsed.getClientId());
        assertEquals(Arrays.asList("OPERATOR"), parsed.getRoles());
        assertEquals(9L, parsed.getTokenVersion());
        assertEquals("100101", parsed.getOrgCode());
    }

    @Test
    public void blacklistedTokenIsRejected() {
        String token = tokenService.issueAccessToken(
                new TokenClaims("u4", "web", Arrays.asList("TERMINAL"), "jti-black", 1L, ""));
        when(redis.hasKey("auth:blacklist:jti-black")).thenReturn(true);

        try {
            tokenService.verifyAccessToken(token);
            fail("blacklisted token should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void logoutAddsAccessTokenToBlacklist() {
        String token = tokenService.issueAccessToken(
                new TokenClaims("u5", "web", Arrays.asList("TERMINAL"), "jti-out", 1L, ""));

        tokenService.revoke("refresh-1", token);

        verify(redis).delete("auth:refresh:refresh-1");
        verify(valueOperations).set(eq("auth:blacklist:jti-out"), eq("1"),
                anyLong(), eq(TimeUnit.MILLISECONDS));
        assertTrue(properties.getAccessTtlMs() > 0);
    }
}
