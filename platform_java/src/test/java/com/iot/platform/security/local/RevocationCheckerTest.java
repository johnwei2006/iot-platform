package com.iot.platform.security.local;

import com.iot.platform.security.AuthServiceClient;
import com.iot.platform.security.AuthServiceException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RevocationCheckerTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RevocationChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new RevocationChecker(redis, true, "auth:", 15_000L, true);
        when(redis.opsForValue()).thenReturn(valueOperations);
    }

    private AuthServiceClient.TokenVerification verification(String jti, String sub, long ver) {
        return new AuthServiceClient.TokenVerification(sub, "web", Arrays.asList("ADMIN"), ver, jti);
    }

    @Test
    public void blacklistedTokenIsRejected() {
        when(redis.hasKey("auth:blacklist:jti-x")).thenReturn(true);

        try {
            checker.check(verification("jti-x", "u1", 1L));
            fail("blacklisted token should be rejected");
        } catch (AuthServiceException e) {
            assertEquals(401, e.getStatus());
        }
    }

    @Test
    public void staleVersionIsRejected() {
        when(redis.hasKey("auth:blacklist:jti-y")).thenReturn(false);
        when(valueOperations.get("auth:userver:u1")).thenReturn("7|ENABLED");

        try {
            checker.check(verification("jti-y", "u1", 3L));
            fail("stale version should be rejected");
        } catch (AuthServiceException e) {
            assertEquals(401, e.getStatus());
        }
    }

    @Test
    public void disabledUserIsRejected() {
        when(redis.hasKey("auth:blacklist:jti-z")).thenReturn(false);
        when(valueOperations.get("auth:userver:u2")).thenReturn("2|DISABLED");

        try {
            checker.check(verification("jti-z", "u2", 2L));
            fail("disabled user should be rejected");
        } catch (AuthServiceException e) {
            assertEquals(401, e.getStatus());
        }
    }

    @Test
    public void redisFailureFailsOpenByDefault() {
        when(redis.hasKey("auth:blacklist:jti-f")).thenThrow(new RuntimeException("redis down"));
        when(redis.opsForValue()).thenThrow(new RuntimeException("redis down"));

        checker.check(verification("jti-f", "u3", 1L)); // fail-open，不抛异常
    }

    @Test
    public void redisFailureFailsClosedWhenConfigured() {
        RevocationChecker strict = new RevocationChecker(redis, true, "auth:", 15_000L, false);
        when(redis.hasKey("auth:blacklist:jti-f")).thenThrow(new RuntimeException("redis down"));

        try {
            strict.check(verification("jti-f", "u3", 1L));
            fail("fail-closed should reject when redis down");
        } catch (AuthServiceException e) {
            assertEquals(503, e.getStatus());
        }
    }

    @Test
    public void disabledCheckerIsNoOp() {
        RevocationChecker off = new RevocationChecker(redis, false, "auth:", 15_000L, true);
        off.check(verification("any", "u1", 999L)); // 不抛异常
    }
}
