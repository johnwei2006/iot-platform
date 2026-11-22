package com.iot.admin.security;

import com.iot.admin.common.AppException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地 JWT 验签（RS256，公钥来自 JWKS/静态公钥）：
 * 完成签名、过期、iss/aud、type=access 校验；撤销检查交由 RevocationChecker。
 * JWKS 未就绪时可回退远程 verify（见 JwtAuthFilter）。
 */
@Component
public class AdminAuthVerifier {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthVerifier.class);
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

    public AdminAuthVerifier(JwksKeyResolver keyResolver,
                             @Value("${admin.auth.issuer:iot-auth}") String issuer,
                             @Value("${admin.auth.audience:iot-platform}") String audience,
                             @Value("${admin.auth.static-key-id:auth-key-1}") String defaultKid) {
        this.keyResolver = keyResolver;
        this.issuer = issuer;
        this.audience = audience;
        this.defaultKid = defaultKid;
    }

    public boolean isReady() {
        return keyResolver.isReady();
    }

    public AdminAuthentication verify(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw AppException.unauthorized("missing access token");
        }
        PublicKey key = keyResolver.forKeyId(extractKid(token));
        if (key == null) {
            throw AppException.unauthorized("no public key for token kid");
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
            throw AppException.unauthorized("invalid or expired token");
        }
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw AppException.unauthorized("invalid token type");
        }
        return new AdminAuthentication(
                claims.getSubject(),
                claims.get(CLAIM_CLIENT, String.class),
                extractRoles(claims),
                claims.get(CLAIM_VER, Number.class) == null
                        ? 0L : claims.get(CLAIM_VER, Number.class).longValue(),
                claims.getId(),
                claims.get(CLAIM_ORG, String.class),
                extractDataScopes(claims));
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Claims claims) {
        List<String> roles = new ArrayList<String>();
        Object raw = claims.get(CLAIM_ROLES);
        if (raw instanceof List) {
            for (Object role : (List<Object>) raw) {
                if (role != null) {
                    roles.add(role.toString());
                }
            }
        }
        return roles;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractDataScopes(Claims claims) {
        Map<String, String> ds = new LinkedHashMap<String, String>();
        Object raw = claims.get(CLAIM_DS);
        if (raw instanceof Map) {
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) raw).entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    ds.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }
        return ds;
    }

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
