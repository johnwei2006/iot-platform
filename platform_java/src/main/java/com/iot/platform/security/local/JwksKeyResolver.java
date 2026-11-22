package com.iot.platform.security.local;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 公钥解析器：从 auth-service 的 JWKS 端点（/.well-known/jwks.json）
 * 拉取 RSA 公钥并按 TTL 缓存（kid 变化即完成密钥平滑轮换）；可选静态 PEM 公钥兜底。
 * 遇到未知 kid 时立即强制刷新 JWKS 再重试；拉取失败按退避间隔重试。
 */
@Component
public class JwksKeyResolver {

    private static final Logger log = LoggerFactory.getLogger(JwksKeyResolver.class);
    /** 拉取失败的退避间隔（毫秒），避免每个请求都打 JWKS 端点 */
    private static final long FAILURE_BACKOFF_MS = 10_000L;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KeyFactory rsaKeyFactory;

    private final String jwksUrl;
    private final long refreshMs;
    private final String staticKeyId;

    private final ConcurrentHashMap<String, PublicKey> keys = new ConcurrentHashMap<String, PublicKey>();
    private volatile long lastFetchAt = 0L;
    private volatile boolean fetchFailed = false;

    public JwksKeyResolver(RestTemplateBuilder builder,
                           @Value("${app.auth-service.base-url:http://localhost:8083}") String baseUrl,
                           @Value("${app.auth.local.jwks-url:}") String jwksUrl,
                           @Value("${app.auth.local.refresh-ms:300000}") long refreshMs,
                           @Value("${app.auth.local.static-public-key:}") String staticPublicKey,
                           @Value("${app.auth.local.static-key-id:auth-key-1}") String staticKeyId) {
        this(builder.setConnectTimeout(2000).setReadTimeout(3000).build(),
                trimTrailingSlash(baseUrl), jwksUrl, refreshMs, staticPublicKey, staticKeyId);
    }

    /** 便于单测注入绑定 MockRestServiceServer 的 RestTemplate。 */
    JwksKeyResolver(RestTemplate restTemplate, String baseUrl, String jwksUrl,
                    long refreshMs, String staticPublicKey, String staticKeyId) {
        this.restTemplate = restTemplate;
        this.jwksUrl = (jwksUrl == null || jwksUrl.trim().isEmpty())
                ? baseUrl + "/.well-known/jwks.json" : jwksUrl.trim();
        this.refreshMs = refreshMs;
        this.staticKeyId = staticKeyId;
        try {
            this.rsaKeyFactory = KeyFactory.getInstance("RSA");
        } catch (Exception e) {
            throw new IllegalStateException("RSA KeyFactory unavailable", e);
        }
        if (staticPublicKey != null && staticPublicKey.contains("PUBLIC KEY")) {
            try {
                keys.put(staticKeyId, parsePem(staticPublicKey));
                log.info("Loaded static JWT public key (kid={})", staticKeyId);
            } catch (Exception e) {
                log.warn("Invalid app.auth.local.static-public-key: {}", e.getMessage());
            }
        }
    }

    @PostConstruct
    public void init() {
        // 构造器已加载静态公钥；此处预留初始化钩子
    }

    /** 是否已有可用公钥（决定 JwtFilter 走本地还是远程验签）。 */
    public boolean isReady() {
        maybeRefresh(false);
        return !keys.isEmpty();
    }

    /** 按 kid 取公钥；未知 kid 触发一次强制刷新。 */
    public PublicKey forKeyId(String kid) {
        maybeRefresh(false);
        PublicKey key = kid == null ? null : keys.get(kid);
        if (key == null) {
            maybeRefresh(true);
            key = kid == null ? null : keys.get(kid);
        }
        return key;
    }

    private synchronized void maybeRefresh(boolean force) {
        long now = System.currentTimeMillis();
        long interval = fetchFailed ? Math.min(refreshMs, FAILURE_BACKOFF_MS) : refreshMs;
        if (!force && now - lastFetchAt < interval) {
            return;
        }
        // 未知 kid 的强制刷新限频，防止伪造 kid 打爆 JWKS 端点
        if (force && now - lastFetchAt < 2000L) {
            return;
        }
        lastFetchAt = now;
        try {
            ResponseEntity<String> response = restTemplate.exchange(jwksUrl, HttpMethod.GET, null, String.class);
            JsonNode keyList = objectMapper.readTree(response.getBody()).path("keys");
            int loaded = 0;
            if (keyList.isArray()) {
                for (JsonNode jwk : keyList) {
                    if (!"RSA".equals(jwk.path("kty").asText())) {
                        continue;
                    }
                    String kid = jwk.path("kid").asText(null);
                    String n = jwk.path("n").asText(null);
                    String e = jwk.path("e").asText(null);
                    if (kid == null || n == null || e == null) {
                        continue;
                    }
                    BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
                    BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
                    keys.put(kid, rsaKeyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent)));
                    loaded++;
                }
            }
            if (loaded > 0) {
                fetchFailed = false;
                log.info("JWKS refreshed from {}: {} key(s) available", jwksUrl, keys.size());
            }
        } catch (Exception e) {
            fetchFailed = true;
            log.warn("JWKS fetch failed from {}: {}", jwksUrl, e.getMessage());
        }
    }

    private PublicKey parsePem(String pem) throws Exception {
        byte[] decoded = Base64.getMimeDecoder().decode(
                pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                        .replaceAll("-----END [A-Z ]+-----", "")
                        .replaceAll("\\s", ""));
        return rsaKeyFactory.generatePublic(new X509EncodedKeySpec(decoded));
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
