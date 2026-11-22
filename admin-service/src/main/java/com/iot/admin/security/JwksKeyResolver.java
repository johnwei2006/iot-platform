package com.iot.admin.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 公钥解析器：从 auth-service JWKS 端点拉取 RSA 公钥并缓存（kid 平滑轮换）；
 * 可选静态 PEM 公钥兜底；未知 kid 强制刷新（限频），失败按退避重试。
 */
@Component
public class JwksKeyResolver {

    private static final Logger log = LoggerFactory.getLogger(JwksKeyResolver.class);
    private static final long FAILURE_BACKOFF_MS = 10_000L;
    private static final long FORCE_REFRESH_MIN_INTERVAL_MS = 2_000L;

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
                           @org.springframework.beans.factory.annotation.Value("${admin.auth.base-url:http://localhost:8083}") String baseUrl,
                           @org.springframework.beans.factory.annotation.Value("${admin.auth.jwks-url:}") String jwksUrl,
                           @org.springframework.beans.factory.annotation.Value("${admin.auth.jwks-refresh-ms:300000}") long refreshMs,
                           @org.springframework.beans.factory.annotation.Value("${admin.auth.static-public-key:}") String staticPublicKey,
                           @org.springframework.beans.factory.annotation.Value("${admin.auth.static-key-id:auth-key-1}") String staticKeyId) {
        this(com.iot.admin.common.RestTemplates.build(builder, 2000, 3000),
                baseUrl, jwksUrl, refreshMs, staticPublicKey, staticKeyId);
    }

    JwksKeyResolver(RestTemplate restTemplate, String baseUrl, String jwksUrl,
                    long refreshMs, String staticPublicKey, String staticKeyId) {
        this.restTemplate = restTemplate;
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        String base = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        this.jwksUrl = (jwksUrl == null || jwksUrl.trim().isEmpty())
                ? base + "/.well-known/jwks.json" : jwksUrl.trim();
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
                log.warn("Invalid static public key: {}", e.getMessage());
            }
        }
    }

    @PostConstruct
    public void init() {
        // 静态公钥已在构造器加载
    }

    public boolean isReady() {
        maybeRefresh(false);
        return !keys.isEmpty();
    }

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
        if (force && now - lastFetchAt < FORCE_REFRESH_MIN_INTERVAL_MS) {
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
}
