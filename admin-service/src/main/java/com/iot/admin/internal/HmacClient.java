package com.iot.admin.internal;

import com.iot.admin.config.AdminProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

/**
 * 内部服务 HMAC 客户端：与 auth-service InternalAuthFilter 共同实现签名约定：
 * X-Signature = HMAC-SHA256(secret, serviceId + "\n" + timestamp + "\n"
 *                            + method + "\n" + path + "\n" + sha256hex(body))
 */
public class HmacClient {

    public static final String HEADER_SERVICE_ID = "X-Service-Id";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Signature";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String serviceId;
    private final String serviceSecret;

    public HmacClient(RestTemplate restTemplate, String baseUrl, String serviceId, String serviceSecret) {
        this.restTemplate = restTemplate;
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        this.baseUrl = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        this.serviceId = serviceId;
        this.serviceSecret = serviceSecret;
    }

    /** 发送带 HMAC 签名的内部请求；非 2xx 抛 IllegalStateException（含响应体摘要）。 */
    public String exchange(String method, String path, String jsonBody) {
        String body = jsonBody == null ? "" : jsonBody;
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = HmacSigner.sign(serviceSecret, serviceId, timestamp,
                method.toUpperCase(), path, body.getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_SERVICE_ID, serviceId);
        headers.set(HEADER_TIMESTAMP, timestamp);
        headers.set(HEADER_SIGNATURE, signature);
        ResponseEntity<String> response = restTemplate.exchange(baseUrl + path,
                HttpMethod.valueOf(method.toUpperCase()),
                new HttpEntity<String>(body, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("internal call failed: " + method + " " + path
                    + " -> " + response.getStatusCode().value());
        }
        return response.getBody();
    }

    public String get(String path) {
        return exchange("GET", path, "");
    }

    public String post(String path, String jsonBody) {
        return exchange("POST", path, jsonBody);
    }

    public String put(String path, String jsonBody) {
        return exchange("PUT", path, jsonBody);
    }

    /** Spring 装配入口。 */
    @Component
    public static class Factory {
        private final RestTemplateBuilderHolder holder;
        private final AdminProperties properties;

        @Autowired
        public Factory(RestTemplateBuilderHolder holder, AdminProperties properties) {
            this.holder = holder;
            this.properties = properties;
        }

        public HmacClient create() {
            return new HmacClient(holder.restTemplate(),
                    properties.getAuth().getBaseUrl(),
                    properties.getAuth().getServiceId(),
                    properties.getAuth().getServiceSecret());
        }
    }

    @org.springframework.stereotype.Component
    public static class RestTemplateBuilderHolder {
        private final org.springframework.boot.web.client.RestTemplateBuilder builder;

        public RestTemplateBuilderHolder(org.springframework.boot.web.client.RestTemplateBuilder builder) {
            this.builder = builder;
        }

        public RestTemplate restTemplate() {
            return com.iot.admin.common.RestTemplates.build(builder, 2000, 3000);
        }
    }
}
