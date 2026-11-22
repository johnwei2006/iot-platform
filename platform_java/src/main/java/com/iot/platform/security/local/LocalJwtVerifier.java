package com.iot.platform.security.local;

import com.iot.platform.security.AuthServiceClient;
import com.iot.platform.security.AuthServiceException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 本地 JWT 验签：用 JWKS/静态公钥在本地完成签名、过期、iss/aud 校验，
 * 不再逐请求远程调用 auth-service（auth-service 故障不阻断业务）。
 * 返回的身份声明交由 {@link RevocationChecker} 做撤销层检查。
 */
@Component
public class LocalJwtVerifier {

    private static final Logger log = LoggerFactory.getLogger(LocalJwtVerifier.class);
    private static final String CLAIM_CLIENT = "client";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_VER = "ver";
    private static final String CLAIM_ORG = "org";
    private static final String CLAIM_DS = "ds";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";

    private final JwksKeyResolver keyResolver;
    private final String issuer;
    private final String audience;
    private final String defaultKid;

    public LocalJwtVerifier(JwksKeyResolver keyResolver,
                            @Value("${app.auth.local.issuer:iot-auth}") String issuer,
                            @Value("${app.auth.local.audience:iot-platform}") String audience,
                            @Value("${app.auth.local.static-key-id:auth-key-1}") String defaultKid) {
        this.keyResolver = keyResolver;
        this.issuer = issuer;
        this.audience = audience;
        this.defaultKid = defaultKid;
    }

    /** 公钥是否就绪（未就绪时 JwtFilter 回退远程验签）。 */
    public boolean isReady() {
        return keyResolver.isReady();
    }

    /** 本地验签；签名/过期/iss/aud/type 任一不符抛 401。 */
    public AuthServiceClient.TokenVerification verify(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw AuthServiceException.unauthorized("missing access token");
        }
        PublicKey key = keyResolver.forKeyId(extractKid(token));
        if (key == null) {
            throw AuthServiceException.unauthorized("no public key for token kid");
        }
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(key)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            throw AuthServiceException.unauthorized("invalid or expired token");
        }
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw AuthServiceException.unauthorized("invalid token type");
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
        java.util.Map<String, String> ds = new java.util.LinkedHashMap<String, String>();
        Object rawDs = claims.get(CLAIM_DS);
        if (rawDs instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawDs).entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    ds.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }
        return new AuthServiceClient.TokenVerification(claims.getSubject(),
                claims.get(CLAIM_CLIENT, String.class), Collections.unmodifiableList(roles), ver,
                claims.getId(), claims.get(CLAIM_ORG, String.class), ds);
    }

    /** 从未验签的 token 头部提取 kid（仅用于选公钥，安全性由后续验签保证）。 */
    private String extractKid(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return defaultKid;
            }
            String header = new String(Base64.getUrlDecoder().decode(parts[0]), "UTF-8");
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(header);
            String kid = node.path("kid").asText(null);
            return kid == null ? defaultKid : kid;
        } catch (Exception e) {
            return defaultKid;
        }
    }
}
