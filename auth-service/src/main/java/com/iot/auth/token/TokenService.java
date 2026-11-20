package com.iot.auth.token;

import com.iot.auth.common.AppException;
import com.iot.auth.common.ResultCode;
import com.iot.auth.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 标准 JWT（HS256）+ Redis Refresh Token + 黑名单。
 *
 * <p>Access Token claims 最小化：sub/clientId/roles/jti/ver/deviceId/iat/exp。
 * Refresh Token 是不透明随机串，存 Redis，可单独撤销；登出还会把 access jti 加入黑名单，
 * 并递增用户 tokenVersion 使全部历史 token 失效。
 */
@Service
public class TokenService {

    private static final String CLAIM_CLIENT = "client";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_VER = "ver";
    private static final String CLAIM_DEVICE = "device";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";

    private final AuthProperties properties;
    private final StringRedisTemplate redis;

    @Autowired
    public TokenService(AuthProperties properties, StringRedisTemplate redis) {
        this.properties = properties;
        this.redis = redis;
    }

    public String issueAccessToken(TokenClaims claims) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(claims.getSubject())
                .setId(claims.getJti())
                .claim(CLAIM_CLIENT, claims.getClientId())
                .claim(CLAIM_ROLES, claims.getRoles())
                .claim(CLAIM_VER, claims.getTokenVersion())
                .claim(CLAIM_DEVICE, claims.getDeviceId() == null ? "" : claims.getDeviceId())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + properties.getAccessTtlMs()))
                .signWith(SignatureAlgorithm.HS256, properties.getJwtSecret().getBytes())
                .compact();
    }

    /** 生成并存储 Refresh Token；返回不透明随机串。 */
    public String issueRefreshToken(String userId, String clientId, String deviceId, long tokenVersion) {
        String refresh = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String value = userId + "|" + clientId + "|"
                + (deviceId == null ? "" : deviceId) + "|" + tokenVersion;
        redis.opsForValue().set(refreshKey(refresh), value,
                properties.getRefreshTtlMs(), TimeUnit.MILLISECONDS);
        return refresh;
    }

    /** 校验 access token；黑名单、过期、签名失败均抛 401。 */
    public TokenClaims verifyAccessToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw AppException.unauthorized("missing access token");
        }
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(properties.getJwtSecret().getBytes())
                    .parseClaimsJws(token)
                    .getBody();
            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw AppException.unauthorized("invalid token type");
            }
            String jti = claims.getId();
            if (jti != null && redis.hasKey(blacklistKey(jti))) {
                throw AppException.unauthorized("token revoked");
            }
            List<String> roles = new ArrayList<String>();
            Object rawRoles = claims.get(CLAIM_ROLES);
            if (rawRoles instanceof List) {
                for (Object role : (List<?>) rawRoles) {
                    if (role != null) {
                        roles.add(role.toString());
                    }
                }
            }
            long ver = claims.get(CLAIM_VER, Number.class) == null
                    ? 0L : claims.get(CLAIM_VER, Number.class).longValue();
            return new TokenClaims(claims.getSubject(),
                    claims.get(CLAIM_CLIENT, String.class), roles, jti, ver,
                    claims.get(CLAIM_DEVICE, String.class));
        } catch (JwtException e) {
            throw AppException.unauthorized("invalid or expired token");
        }
    }

    /** 读取并解析 refresh token 的 Redis 记录；用户版本与角色由 AuthService 校验/加载。 */
    public RefreshData readRefresh(String refreshToken) {
        String value = redis.opsForValue().get(refreshKey(refreshToken));
        if (value == null) {
            throw AppException.unauthorized("invalid or expired refresh token");
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length != 4) {
            throw AppException.unauthorized("malformed refresh token");
        }
        return new RefreshData(parts[0], parts[1], parts[2], Long.parseLong(parts[3]));
    }

    /** 基于 refresh 记录签发新的 access token。 */
    public String issueAccessFromRefresh(RefreshData data, List<String> roles) {
        return issueAccessToken(new TokenClaims(data.getUserId(), data.getClientId(), roles,
                UUID.randomUUID().toString(), data.getVersion(), data.getDeviceId()));
    }

    /** 登出：删除 refresh、把 access jti 加入黑名单直到过期。 */
    public void revoke(String refreshToken, String accessToken) {
        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            redis.delete(refreshKey(refreshToken));
        }
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(properties.getJwtSecret().getBytes())
                        .parseClaimsJws(accessToken)
                        .getBody();
                long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (remaining > 0 && claims.getId() != null) {
                    redis.opsForValue().set(blacklistKey(claims.getId()), "1",
                            remaining, TimeUnit.MILLISECONDS);
                }
            } catch (JwtException ignored) {
                // 无效 token 无需加入黑名单
            }
        }
    }

    private String refreshKey(String refreshToken) {
        return properties.getRedisPrefix() + "refresh:" + refreshToken;
    }

    private String blacklistKey(String jti) {
        return properties.getRedisPrefix() + "blacklist:" + jti;
    }

    /** refresh token 在 Redis 中的解析结果。 */
    public static class RefreshData {
        private final String userId;
        private final String clientId;
        private final String deviceId;
        private final long version;

        public RefreshData(String userId, String clientId, String deviceId, long version) {
            this.userId = userId;
            this.clientId = clientId;
            this.deviceId = deviceId;
            this.version = version;
        }

        public String getUserId() { return userId; }
        public String getClientId() { return clientId; }
        public String getDeviceId() { return deviceId; }
        public long getVersion() { return version; }
    }
}
