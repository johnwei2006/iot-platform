package com.iot.auth.api;

import com.iot.auth.audit.AuditService;
import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import com.iot.auth.identity.User;
import com.iot.auth.identity.UserRepository;
import com.iot.auth.token.TokenClaims;
import com.iot.auth.token.TokenService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenService tokenService;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private LoginRateLimiter rateLimiter;
    @Mock
    private AuditService auditService;

    private AuthProperties properties;
    private AuthService authService;

    @Before
    public void setUp() {
        properties = new AuthProperties();
        properties.setJwtSecret("test-secret-at-least-32-characters-long-1234567890");
        properties.setRedisPrefix("auth:");
        when(redis.opsForValue()).thenReturn(valueOperations);
        authService = new AuthService(userRepository, tokenService, properties, redis,
                rateLimiter, auditService);
    }

    private User user(String id, long tokenVersion) {
        User user = new User();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setRoles(Arrays.asList("ADMIN"));
        user.setTokenVersion(tokenVersion);
        user.setStatus("ENABLED");
        return user;
    }

    @Test
    public void refreshRotatesAndIssuesNewRefreshToken() {
        TokenService.RefreshData data = new TokenService.RefreshData("u1", "web", "d1", 2L);
        when(tokenService.rotateRefresh("old")).thenReturn(data);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", 2L)));
        when(tokenService.issueAccessFromRefresh(eq(data), anyList())).thenReturn("new-access");
        when(tokenService.issueRefreshToken("u1", "web", "d1", 2L)).thenReturn("new-refresh");

        AuthDtos.RefreshRequest request = new AuthDtos.RefreshRequest();
        request.setRefreshToken("old");
        AuthDtos.TokenResponse response = authService.refresh(request, null);

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
    }

    @Test
    public void refreshFallsBackToCookieToken() {
        TokenService.RefreshData data = new TokenService.RefreshData("u1", "web", "d1", 2L);
        when(tokenService.rotateRefresh("cookie-token")).thenReturn(data);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", 2L)));
        when(tokenService.issueAccessFromRefresh(eq(data), anyList())).thenReturn("new-access");
        when(tokenService.issueRefreshToken(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn("new-refresh");

        AuthDtos.RefreshRequest request = new AuthDtos.RefreshRequest();
        request.setRefreshToken("");
        AuthDtos.TokenResponse response = authService.refresh(request, "cookie-token");

        assertEquals("new-refresh", response.getRefreshToken());
    }

    @Test
    public void missingRefreshIsRejected() {
        try {
            authService.refresh(new AuthDtos.RefreshRequest(), null);
            fail("missing refresh should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void refreshReuseRevokesAllSessions() {
        when(tokenService.rotateRefresh("stolen")).thenThrow(new TokenService.RefreshReuseException("u1"));
        User victim = user("u1", 3L);
        when(userRepository.findById("u1")).thenReturn(Optional.of(victim));

        AuthDtos.RefreshRequest request = new AuthDtos.RefreshRequest();
        request.setRefreshToken("stolen");
        try {
            authService.refresh(request, null);
            fail("refresh reuse should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
        assertEquals(4L, victim.getTokenVersion());
        verify(userRepository).save(victim);
        verify(redis).delete("auth:userver:u1");
    }

    @Test
    public void verifyAcceptsCurrentVersionFromCache() {
        when(tokenService.verifyAccessToken("token")).thenReturn(
                new TokenClaims("u1", "web", Arrays.asList("ADMIN"), "jti-1", 2L, ""));
        when(valueOperations.get("auth:userver:u1")).thenReturn("2|ENABLED");

        AuthDtos.VerifyResponse response = authService.verify("token");

        assertEquals("u1", response.getSubject());
        assertEquals(2L, response.getTokenVersion());
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    public void verifyRejectsStaleVersion() {
        when(tokenService.verifyAccessToken("token")).thenReturn(
                new TokenClaims("u1", "web", Arrays.asList("ADMIN"), "jti-1", 2L, ""));
        when(valueOperations.get("auth:userver:u1")).thenReturn("5|ENABLED");

        try {
            authService.verify("token");
            fail("stale token version should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void verifyRejectsDisabledUser() {
        when(tokenService.verifyAccessToken("token")).thenReturn(
                new TokenClaims("u1", "web", Arrays.asList("ADMIN"), "jti-1", 2L, ""));
        when(valueOperations.get("auth:userver:u1")).thenReturn("2|DISABLED");

        try {
            authService.verify("token");
            fail("disabled user should be rejected");
        } catch (AppException e) {
            assertEquals(401, e.getResultCode().getCode());
        }
    }

    @Test
    public void verifyLoadsAndCachesStateOnCacheMiss() {
        when(tokenService.verifyAccessToken("token")).thenReturn(
                new TokenClaims("u1", "web", Arrays.asList("ADMIN"), "jti-1", 2L, ""));
        when(valueOperations.get("auth:userver:u1")).thenReturn(null);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", 2L)));

        authService.verify("token");

        verify(valueOperations).set(eq("auth:userver:u1"), eq("2|ENABLED"),
                anyLong(), eq(java.util.concurrent.TimeUnit.MILLISECONDS));
    }

    @Test
    public void revokeAllBumpsTokenVersionAndEvictsCache() {
        User user = user("u1", 7L);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        authService.revokeAllSessions("u1", "10.0.0.1");

        assertEquals(8L, user.getTokenVersion());
        verify(userRepository).save(user);
        verify(redis).delete("auth:userver:u1");
        verify(auditService).record(eq("REVOKE_ALL"), eq("u1"), eq("user-u1"),
                eq(null), eq("10.0.0.1"), anyString());
    }

    @Test
    public void revokeAllUnknownUserIsRejected() {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());
        try {
            authService.revokeAllSessions("ghost", null);
            fail("unknown user should be rejected");
        } catch (AppException e) {
            assertEquals(404, e.getResultCode().getCode());
        }
    }

    @Test
    public void loginChecksRateLimitAndAuditsSuccess() {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest();
        request.setUsername("alice");
        request.setPassword("pw");
        request.setClientId("web");
        User alice = user("u1", 1L);
        alice.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("pw"));
        AuthProperties.Client webClient = new AuthProperties.Client();
        webClient.setId("web");
        properties.setClients(java.util.Collections.singletonList(webClient));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(tokenService.issueRefreshToken(anyString(), anyString(),
                org.mockito.ArgumentMatchers.any(), anyLong())).thenReturn("refresh");
        when(tokenService.issueAccessToken(org.mockito.ArgumentMatchers.any(TokenClaims.class)))
                .thenReturn("access");

        authService.login(request, "10.0.0.9");

        verify(rateLimiter).check("alice", "10.0.0.9");
        verify(rateLimiter).recordSuccess("alice", "10.0.0.9");
        verify(auditService).record(eq("LOGIN_SUCCESS"), eq("u1"), eq("alice"),
                eq("web"), eq("10.0.0.9"), eq(null));
    }
}
