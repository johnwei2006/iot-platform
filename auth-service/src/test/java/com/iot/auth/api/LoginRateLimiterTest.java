package com.iot.auth.api;

import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoginRateLimiterTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthProperties properties;
    private LoginRateLimiter limiter;

    @Before
    public void setUp() {
        properties = new AuthProperties();
        properties.setRedisPrefix("auth:");
        properties.getRateLimit().setMaxFailures(2);
        when(redis.opsForValue()).thenReturn(valueOperations);
        limiter = new LoginRateLimiter(properties, redis);
    }

    @Test
    public void underLimitPasses() {
        when(valueOperations.get("auth:loginfail:user:alice")).thenReturn("2");
        when(valueOperations.get("auth:loginfail:ip:1.1.1.1")).thenReturn("1");

        limiter.check("alice", "1.1.1.1"); // 不抛异常
    }

    @Test
    public void overUserLimitThrows429() {
        when(valueOperations.get("auth:loginfail:user:alice")).thenReturn("3");
        when(valueOperations.get("auth:loginfail:ip:1.1.1.1")).thenReturn("0");

        try {
            limiter.check("alice", "1.1.1.1");
            fail("over limit should be rejected");
        } catch (AppException e) {
            assertEquals(429, e.getResultCode().getCode());
        }
    }

    @Test
    public void overIpLimitThrows429() {
        when(valueOperations.get("auth:loginfail:user:alice")).thenReturn("0");
        when(valueOperations.get("auth:loginfail:ip:1.1.1.1")).thenReturn("5");

        try {
            limiter.check("alice", "1.1.1.1");
            fail("over ip limit should be rejected");
        } catch (AppException e) {
            assertEquals(429, e.getResultCode().getCode());
        }
    }

    @Test
    public void recordFailureIncrementsAndSetsTtl() {
        when(valueOperations.increment("auth:loginfail:user:alice")).thenReturn(1L);

        limiter.recordFailure("alice", "1.1.1.1");

        verify(redis).expire(eq("auth:loginfail:user:alice"), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(valueOperations).increment("auth:loginfail:ip:1.1.1.1");
    }

    @Test
    public void recordSuccessClearsCounters() {
        limiter.recordSuccess("alice", "1.1.1.1");
        verify(redis).delete("auth:loginfail:user:alice");
        verify(redis).delete("auth:loginfail:ip:1.1.1.1");
    }

    @Test
    public void disabledLimiterIsNoOp() {
        properties.getRateLimit().setEnabled(false);

        limiter.check("alice", "1.1.1.1");
        verify(valueOperations, never()).increment(anyString());
    }
}
