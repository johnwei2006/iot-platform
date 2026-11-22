package com.iot.admin.security;

import com.iot.admin.common.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地撤销层检查（与 auth-service 共享 Redis auth: 前缀）：
 * 黑名单 jti + 全局 tokenVersion/用户状态；15 秒内存缓存；Redis 异常 fail-open 可配置。
 */
@Component
public class RevocationChecker {

    private static final Logger log = LoggerFactory.getLogger(RevocationChecker.class);
    private static final String STATE_MISS = "miss";

    private final StringRedisTemplate redis;
    private final String prefix;
    private final long cacheTtlMs;
    private final boolean failOpen;
    private final boolean enabled;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<String, CacheEntry>();

    public RevocationChecker(StringRedisTemplate redis,
                             @Value("${admin.auth.revocation.enabled:true}") boolean enabled,
                             @Value("${admin.auth.revocation.redis-prefix:auth:}") String prefix,
                             @Value("${admin.auth.revocation.cache-ttl-ms:15000}") long cacheTtlMs,
                             @Value("${admin.auth.revocation.fail-open:true}") boolean failOpen) {
        this.redis = redis;
        this.prefix = prefix;
        this.cacheTtlMs = cacheTtlMs;
        this.failOpen = failOpen;
        this.enabled = enabled;
        if (!enabled) {
            log.warn("Admin revocation check is DISABLED (admin.auth.revocation.enabled=false)");
        }
    }

    public void check(AdminAuthentication auth) {
        if (!enabled || auth == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (auth.getJti() != null) {
            String key = "jti:" + auth.getJti();
            CacheEntry entry = cache.get(key);
            if (entry != null && now < entry.expireAt) {
                if (entry.revoked) {
                    throw AppException.unauthorized("token revoked");
                }
            } else {
                boolean revoked;
                try {
                    revoked = redis.hasKey(prefix + "blacklist:" + auth.getJti());
                } catch (RuntimeException e) {
                    logRedisFailure("blacklist check", e);
                    return;
                }
                cache.put(key, new CacheEntry(revoked, now + cacheTtlMs));
                if (revoked) {
                    throw AppException.unauthorized("token revoked");
                }
            }
        }
        if (auth.getSubject() != null) {
            String key = "sub:" + auth.getSubject();
            CacheEntry entry = cache.get(key);
            String state;
            if (entry != null && now < entry.expireAt) {
                state = entry.state;
            } else {
                try {
                    state = redis.opsForValue().get(prefix + "userver:" + auth.getSubject());
                } catch (RuntimeException e) {
                    logRedisFailure("session state check", e);
                    return;
                }
                cache.put(key, new CacheEntry(state == null ? STATE_MISS : state, now + cacheTtlMs));
            }
            if (state != null && !STATE_MISS.equals(state) && !state.isEmpty()) {
                String[] parts = state.split("\\|", -1);
                if (parts.length == 2) {
                    try {
                        if (Long.parseLong(parts[0]) != auth.getTokenVersion()) {
                            throw AppException.unauthorized("token revoked");
                        }
                        if (!"ENABLED".equals(parts[1])) {
                            throw AppException.unauthorized("user disabled");
                        }
                    } catch (NumberFormatException ignored) {
                        // 状态格式异常，放行
                    }
                }
            }
        }
    }

    private void logRedisFailure(String action, RuntimeException e) {
        if (failOpen) {
            log.warn("Redis unavailable during {}, failing open: {}", action, e.getMessage());
        } else {
            throw new AppException(com.iot.admin.common.ResultCode.ERROR, "revocation store unavailable");
        }
    }

    private static class CacheEntry {
        final boolean revoked;
        final String state;
        final long expireAt;

        CacheEntry(boolean revoked, long expireAt) {
            this.revoked = revoked;
            this.state = null;
            this.expireAt = expireAt;
        }

        CacheEntry(String state, long expireAt) {
            this.revoked = false;
            this.state = state;
            this.expireAt = expireAt;
        }
    }
}
