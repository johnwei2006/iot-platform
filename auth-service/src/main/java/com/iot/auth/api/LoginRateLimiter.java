package com.iot.auth.api;

import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 登录失败限流：同一用户名或来源 IP 在窗口内失败次数超限即拒绝（429），
 * 防止撞库与暴力破解。登录成功即清零。
 */
@Component
public class LoginRateLimiter {

    private final AuthProperties properties;
    private final StringRedisTemplate redis;

    @Autowired
    public LoginRateLimiter(AuthProperties properties, StringRedisTemplate redis) {
        this.properties = properties;
        this.redis = redis;
    }

    /** 校验是否超限；超限抛 429。 */
    public void check(String username, String ip) {
        AuthProperties.RateLimit cfg = properties.getRateLimit();
        if (cfg == null || !cfg.isEnabled()) {
            return;
        }
        long userFailures = currentCount(userKey(username));
        long ipFailures = currentCount(ipKey(ip));
        if (userFailures > cfg.getMaxFailures() || ipFailures > cfg.getMaxFailures()) {
            throw AppException.rateLimited("too many login failures, try later");
        }
    }

    /** 登录失败：计数 +1 并设置窗口 TTL。 */
    public void recordFailure(String username, String ip) {
        AuthProperties.RateLimit cfg = properties.getRateLimit();
        if (cfg == null || !cfg.isEnabled()) {
            return;
        }
        increment(userKey(username), cfg.getWindowMs());
        increment(ipKey(ip), cfg.getWindowMs());
    }

    /** 登录成功：清零计数。 */
    public void recordSuccess(String username, String ip) {
        redis.delete(userKey(username));
        redis.delete(ipKey(ip));
    }

    private long currentCount(String key) {
        String value = redis.opsForValue().get(key);
        try {
            return value == null ? 0L : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void increment(String key, long windowMs) {
        if (key == null) {
            return;
        }
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, windowMs, TimeUnit.MILLISECONDS);
        }
    }

    private String userKey(String username) {
        return username == null || username.trim().isEmpty()
                ? null : properties.getRedisPrefix() + "loginfail:user:" + username;
    }

    private String ipKey(String ip) {
        return ip == null || ip.trim().isEmpty()
                ? null : properties.getRedisPrefix() + "loginfail:ip:" + ip;
    }
}
