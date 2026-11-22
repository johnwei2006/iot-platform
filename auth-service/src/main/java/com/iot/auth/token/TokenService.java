package com.iot.auth.token;

import com.iot.auth.common.AppException;
import com.iot.auth.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.MacAlgorithm;
import io.jsonwebtoken.security.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 标准 JWT（RS256 非对称签名优先，HS256 兼容模式）+ Redis Refresh Token + 黑名单。
 *
 * <p>Access Token claims 最小化：sub/clientId/roles/jti/ver/deviceId/iat/exp/iss/aud。
 * RS256 模式：auth-service 持私钥签名，下游服务经 JWKS（/.well-known/jwks.json）
 * 获取公钥本地验签，不再逐请求远程调用；kid 头支持密钥平滑轮换。
 * HS256 模式：共享密钥，保留兼容。Refresh Token 是不透明随机串，存 Redis，可单独撤销；
 * 登出还会把 access jti 加入黑名单，并递增用户 tokenVersion 使全部历史 token 失效。
 */
@Service
public class TokenService {

    private static final String CLAIM_CLIENT = "client";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_VER = "ver";
    private static final String CLAIM_DEVICE = "device";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ORG = "org";
    private static final String CLAIM_DS = "ds";
    private static final String TYPE_ACCESS = "access";

    private final AuthProperties properties;
    private final StringRedisTemplate redis;

    /** true=RS256（私钥签名）；false=HS256（共享密钥兼容模式） */
    private boolean rsaMode;
    private io.jsonwebtoken.security.SignatureAlgorithm rsaAlgorithm;
    private io.jsonwebtoken.security.MacAlgorithm macAlgorithm;
    private KeyFactory rsaKeyFactory;
    private PrivateKey rsaPrivateKey;
    private PublicKey rsaPublicKey;
    private javax.crypto.SecretKey hmacKey;

    @Autowired
    public TokenService(AuthProperties properties, StringRedisTemplate redis) {
        this.properties = properties;
        this.redis = redis;
    }

    @PostConstruct
    public void initCrypto() {
        rsaMode = properties.isRsaMode();
        if (rsaMode) {
            try {
                rsaKeyFactory = KeyFactory.getInstance("RSA");
                byte[] decoded = Base64.getMimeDecoder().decode(pemBody(properties.getJwtPrivateKey()));
                RSAPrivateCrtKey crt = (RSAPrivateCrtKey) rsaKeyFactory
                        .generatePrivate(new PKCS8EncodedKeySpec(decoded));
                rsaPrivateKey = crt;
                rsaPublicKey = rsaKeyFactory.generatePublic(
                        new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
                rsaAlgorithm = Jwts.SIG.RS256;
            } catch (Exception e) {
                throw new IllegalStateException("invalid auth.jwt-private-key (PKCS#8 RSA PEM required)", e);
            }
        } else {
            macAlgorithm = Jwts.SIG.HS256;
            hmacKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                    properties.getJwtSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    public String issueAccessToken(TokenClaims claims) {
        Date now = new Date();
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .subject(claims.getSubject())
                .id(claims.getJti())
                .claim(CLAIM_CLIENT, claims.getClientId())
                .claim(CLAIM_ROLES, claims.getRoles())
                .claim(CLAIM_VER, claims.getTokenVersion())
                .claim(CLAIM_DEVICE, claims.getDeviceId() == null ? "" : claims.getDeviceId())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ORG, claims.getOrgCode() == null ? "" : claims.getOrgCode())
                .claim(CLAIM_DS, claims.getDataScopes())
                .issuer(properties.getJwtIssuer())
                .audience().add(properties.getJwtAudience()).and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + properties.getAccessTtlMs()));
        if (rsaMode) {
            builder.header().add("kid", properties.getJwtKeyId()).and();
            builder.signWith(rsaPrivateKey, rsaAlgorithm);
        } else {
            builder.signWith(hmacKey, macAlgorithm);
        }
        return builder.compact();
    }

    /** RS256 模式下返回公钥 JWK（kid/n/e，base64url）；HS256 模式返回 null。 */
    public Map<String, Object> publicJwk() {
        if (!rsaMode || !(rsaPublicKey instanceof java.security.interfaces.RSAPublicKey)) {
            return null;
        }
        java.security.interfaces.RSAPublicKey key = (java.security.interfaces.RSAPublicKey) rsaPublicKey;
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        Map<String, Object> jwk = new LinkedHashMap<String, Object>();
        jwk.put("kty", "RSA");
        jwk.put("kid", properties.getJwtKeyId());
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("n", encoder.encodeToString(key.getModulus().toByteArray()));
        jwk.put("e", encoder.encodeToString(key.getPublicExponent().toByteArray()));
        return jwk;
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

    /** 校验 access token；签名/过期/iss/aud 不符、黑名单命中均抛 401。 */
    public TokenClaims verifyAccessToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw AppException.unauthorized("missing access token");
        }
        Claims claims;
        try {
            claims = parseAccessToken(token);
        } catch (JwtException e) {
            throw AppException.unauthorized("invalid or expired token");
        }
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
        Map<String, String> ds = new LinkedHashMap<String, String>();
        Object rawDs = claims.get(CLAIM_DS);
        if (rawDs instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawDs).entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    ds.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }
        return new TokenClaims(claims.getSubject(),
                claims.get(CLAIM_CLIENT, String.class), roles, jti, ver,
                claims.get(CLAIM_DEVICE, String.class),
                claims.get(CLAIM_ORG, String.class), ds);
    }

    /** 解析并验签 access token（含 iss/aud 强校验），不查黑名单。 */
    public Claims parseAccessToken(String token) {
        io.jsonwebtoken.JwtParserBuilder parserBuilder = Jwts.parser()
                .requireIssuer(properties.getJwtIssuer())
                .requireAudience(properties.getJwtAudience());
        io.jsonwebtoken.JwtParser parser = rsaMode
                ? parserBuilder.verifyWith(rsaPublicKey).build()
                : parserBuilder.verifyWith(hmacKey).build();
        return parser.parseSignedClaims(token).getPayload();
    }

    /**
     * 消费（轮换）refresh token：读取记录后立即删除旧键并写入墓碑，返回解析结果。
     *
     * <p>已轮换的 token 再次提交时命中墓碑，判定为重用（token 泄露特征），
     * 抛出 {@link RefreshReuseException}，由调用方撤销该用户全部会话。
     */
    public RefreshData rotateRefresh(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw AppException.unauthorized("missing refresh token");
        }
        String value = redis.opsForValue().get(refreshKey(refreshToken));
        if (value == null) {
            String reused = redis.opsForValue().get(usedRefreshKey(refreshToken));
            if (reused != null) {
                throw new RefreshReuseException(reused);
            }
            throw AppException.unauthorized("invalid or expired refresh token");
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length != 4) {
            throw AppException.unauthorized("malformed refresh token");
        }
        redis.delete(refreshKey(refreshToken));
        redis.opsForValue().set(usedRefreshKey(refreshToken), parts[0],
                properties.getRefreshTtlMs(), TimeUnit.MILLISECONDS);
        return new RefreshData(parts[0], parts[1], parts[2], Long.parseLong(parts[3]));
    }

    /** 基于 refresh 记录签发新的 access token（org/ds 以用户当前值为准）。 */
    public String issueAccessFromRefresh(RefreshData data, List<String> roles,
                                         String orgCode, Map<String, String> dataScopes) {
        return issueAccessToken(new TokenClaims(data.getUserId(), data.getClientId(), roles,
                UUID.randomUUID().toString(), data.getVersion(), data.getDeviceId(),
                orgCode, dataScopes));
    }

    /** 登出：删除 refresh、把 access jti 加入黑名单直到过期。 */
    public void revoke(String refreshToken, String accessToken) {
        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            redis.delete(refreshKey(refreshToken));
        }
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            try {
                Claims claims = parseAccessToken(accessToken);
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

    private String pemBody(String pem) {
        return pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
    }

    private String refreshKey(String refreshToken) {
        return properties.getRedisPrefix() + "refresh:" + refreshToken;
    }

    private String usedRefreshKey(String refreshToken) {
        return properties.getRedisPrefix() + "refresh-used:" + refreshToken;
    }

    private String blacklistKey(String jti) {
        return properties.getRedisPrefix() + "blacklist:" + jti;
    }

    /** refresh token 重用（已轮换的 token 再次提交）异常，携带用户 id 供全局撤销。 */
    public static class RefreshReuseException extends RuntimeException {

        private final String userId;

        public RefreshReuseException(String userId) {
            super("refresh token reuse detected");
            this.userId = userId;
        }

        public String getUserId() { return userId; }
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
