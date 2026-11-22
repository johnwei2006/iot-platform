package com.iot.platform.security.local;

import com.iot.platform.security.AuthServiceClient;
import com.iot.platform.security.AuthServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地撤销层检查（验签通过后）：
 * <ul>
 *   <li>黑名单：Redis {@code <prefix>blacklist:<jti>} 存在即已登出/被拉黑；</li>
 *   <li>全局撤销：Redis {@code <prefix>userver:<sub>} 为 "version|status"，
 *       与 token 内 ver 不一致或用户被禁用即拒绝。</li>
 * </ul>
 * 结果带短 TTL 内存缓存；Redis 异常按 fail-open（可配置 fail-closed）。
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
                             @Value("${app.auth.local.revocation.enabled:true}") boolean enabled,
                             @Value("${app.auth.local.revocation.redis-prefix:auth:}") String prefix,
                             @Value("${app.auth.local.revocation.cache-ttl-ms:15000}") long cacheTtlMs,
                             @Value("${app.auth.local.revocation.fail-open:true}") boolean failOpen) {
        this.redis = redis;
        this.prefix = prefix;
        this.cacheTtlMs = cacheTtlMs;
        this.failOpen = failOpen;
        this.enabled = enabled;
        if (!enabled) {
            log.warn("Local revocation check is DISABLED (app.auth.local.revocation.enabled=false)");
        }
    }

    public void check(AuthServiceClient.TokenVerification verification) {
        if (!enabled || verification == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (verification.getJti() != null) {
            checkJti(verification, now);
        }
        if (verification.getSubject() != null) {
            checkSubjectState(verification, now);
        }
    }

    private void checkJti(AuthServiceClient.TokenVerification verification, long now) {
        String key = "jti:" + verification.getJti();
        CacheEntry entry = cache.get(key);
        if (entry != null && now < entry.expireAt) {
            if (entry.revoked) {
                throw AuthServiceException.unauthorized("token revoked");
            }
            return;
        }
        boolean revoked;
        try {
            revoked = redis.hasKey(prefix + "blacklist:" + verification.getJti());
        } catch (RuntimeException e) {
            logRedisFailure("blacklist check", e);
            return; // fail-open（或由调用方按配置 fail-closed）
        }
        cache.put(key, new CacheEntry(revoked, now + cacheTtlMs));
        if (revoked) {
            throw AuthServiceException.unauthorized("token revoked");
        }
    }

    private void checkSubjectState(AuthServiceClient.TokenVerification verification, long now) {
        String key = "sub:" + verification.getSubject();
        CacheEntry entry = cache.get(key);
        String state;
        if (entry != null && now < entry.expireAt) {
            state = entry.state;
        } else {
            try {
                state = redis.opsForValue().get(prefix + "userver:" + verification.getSubject());
            } catch (RuntimeException e) {
                logRedisFailure("session state check", e);
                return;
            }
            cache.put(key, new CacheEntry(state == null ? STATE_MISS : state, now + cacheTtlMs));
        }
        if (state == null || STATE_MISS.equals(state) || state.isEmpty()) {
            return; // 无快照可比对，依赖黑名单层
        }
        String[] parts = state.split("\\|", -1);
        if (parts.length != 2) {
            return;
        }
        try {
            long currentVersion = Long.parseLong(parts[0]);
            if (currentVersion != verification.getTokenVersion()) {
                throw AuthServiceException.unauthorized("token revoked");
            }
            if (!"ENABLED".equals(parts[1])) {
                throw AuthServiceException.unauthorized("user disabled");
            }
        } catch (AuthServiceException e) {
            throw e;
        } catch (NumberFormatException ignored) {
            // 状态格式异常，放行
        }
    }

    private void logRedisFailure(String action, RuntimeException e) {
        if (failOpen) {
            log.warn("Redis unavailable during {}, failing open: {}", action, e.getMessage());
        } else {
            log.warn("Redis unavailable during {}, failing closed", action);
            throw AuthServiceException.unavailable("revocation store unavailable: " + e.getMessage());
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
