package com.iot.api.auth;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign configuration for internal HMAC-signed calls.
 * Reads service id/secret from the consumer's application properties:
 *   auth.service.internal.service-id
 *   auth.service.internal.service-secret
 */
@Configuration
public class InternalHmacRequestInterceptor {

    @Value("${auth.service.internal.service-id:admin_service}")
    private String serviceId;

    @Value("${auth.service.internal.service-secret:}")
    private String serviceSecret;

    @Bean
    public RequestInterceptor internalHmacInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String path = template.path();
                if (path == null || path.isEmpty()) path = "/";
                String body = template.body() == null ? "" : new String(template.body(), java.nio.charset.StandardCharsets.UTF_8);
                String signature = HmacSigner.sign(serviceSecret, serviceId, timestamp,
                        template.method(), path, body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                template.header(HmacSigner.HEADER_SERVICE_ID, serviceId);
                template.header(HmacSigner.HEADER_TIMESTAMP, timestamp);
                template.header(HmacSigner.HEADER_SIGNATURE, signature);
            }
        };
    }
}
