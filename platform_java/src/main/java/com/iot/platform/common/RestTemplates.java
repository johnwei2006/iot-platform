package com.iot.platform.common;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 构建工具：Spring Boot 3 移除了 RestTemplateBuilder 的
 * setConnectTimeout/setReadTimeout，改用 SimpleClientHttpRequestFactory 统一设置超时。
 */
public final class RestTemplates {

    private RestTemplates() {
    }

    public static RestTemplate build(RestTemplateBuilder builder, int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return builder.requestFactory(() -> factory).build();
    }
}
