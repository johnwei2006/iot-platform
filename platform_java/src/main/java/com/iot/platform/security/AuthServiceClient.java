package com.iot.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * auth-service 远程鉴权客户端。
 *
 * <p>两个职责：
 * <ol>
 *   <li>{@link #verify(String)}：把 access token 交给 auth-service 的 {@code /api/auth/verify}
 *       验签，由认证服务统一检查签名、过期、黑名单和 tokenVersion，返回身份声明；</li>
 *   <li>{@link #post(String, String)}：为 {@code /api/auth/**} 兼容入口转发登录/刷新/登出请求。</li>
 * </ol>
 * 不在本地保存 JWT 密钥，也不再签发令牌。任何认证服务不可达或校验失败都按 fail-closed 处理。
 */
@Component
public class AuthServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String verifyUrl;

    public AuthServiceClient(RestTemplateBuilder builder,
                             @Value("${app.auth-service.base-url:http://localhost:8083}") String baseUrl,
                             @Value("${app.auth-service.verify-path:/api/auth/verify}") String verifyPath,
                             @Value("${app.auth-service.connect-timeout-ms:2000}") int connectTimeoutMs,
                             @Value("${app.auth-service.read-timeout-ms:2000}") int readTimeoutMs) {
        this(builder.setConnectTimeout(connectTimeoutMs).setReadTimeout(readTimeoutMs).build(),
                baseUrl, verifyPath);
    }

    /** 便于单测注入 MockRestServiceServer 绑定的 RestTemplate。 */
    AuthServiceClient(RestTemplate restTemplate, String baseUrl, String verifyPath) {
        this.restTemplate = restTemplate;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.verifyUrl = this.baseUrl + withLeadingSlash(verifyPath);
    }

    /** 远程验签；成功返回身份声明，失败抛 {@link AuthServiceException}。 */
    public TokenVerification verify(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw AuthServiceException.unauthorized("missing access token");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        try {
            ResponseEntity<VerifyEnvelope> response = restTemplate.exchange(
                    verifyUrl, HttpMethod.POST, new HttpEntity<Void>(headers), VerifyEnvelope.class);
            return toVerification(response.getBody());
        } catch (HttpStatusCodeException e) {
            throw mapHttpError(e);
        } catch (RestClientException e) {
            throw AuthServiceException.unavailable("auth service unavailable: " + e.getMessage());
        }
    }

    /** 透明转发兼容认证请求，保留 auth-service 的 HTTP 状态码与原始响应体。 */
    public ProxyResponse post(String path, String jsonBody) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + withLeadingSlash(path), HttpMethod.POST,
                    new HttpEntity<String>(jsonBody, jsonHeaders()), String.class);
            return new ProxyResponse(response.getStatusCodeValue(), response.getBody());
        } catch (HttpStatusCodeException e) {
            return new ProxyResponse(e.getRawStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw AuthServiceException.unavailable("auth service unavailable: " + e.getMessage());
        }
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private TokenVerification toVerification(VerifyEnvelope envelope) {
        if (envelope == null) {
            throw AuthServiceException.unauthorized("empty verification response");
        }
        if (envelope.getCode() != 200 || envelope.getData() == null) {
            String message = envelope.getMessage() == null ? "invalid access token" : envelope.getMessage();
            throw AuthServiceException.unauthorized(message);
        }
        VerifyData data = envelope.getData();
        if (data.getSubject() == null || data.getSubject().trim().isEmpty()) {
            throw AuthServiceException.unauthorized("invalid token subject");
        }
        List<String> roles = data.getRoles() == null ? Collections.<String>emptyList() : data.getRoles();
        return new TokenVerification(data.getSubject(), data.getClientId(), roles, data.getTokenVersion());
    }

    private AuthServiceException mapHttpError(HttpStatusCodeException e) {
        if (e.getStatusCode().series() == HttpStatus.Series.SERVER_ERROR) {
            return AuthServiceException.unavailable("auth service error: " + e.getRawStatusCode());
        }
        return AuthServiceException.unauthorized("invalid access token");
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "http://localhost:8083";
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String withLeadingSlash(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }
        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    /** 验签结果的身份声明。 */
    public static class TokenVerification {
        private final String subject;
        private final String clientId;
        private final List<String> roles;
        private final long tokenVersion;

        public TokenVerification(String subject, String clientId, List<String> roles, long tokenVersion) {
            this.subject = subject;
            this.clientId = clientId;
            this.roles = roles;
            this.tokenVersion = tokenVersion;
        }

        public String getSubject() { return subject; }
        public String getClientId() { return clientId; }
        public List<String> getRoles() { return roles; }
        public long getTokenVersion() { return tokenVersion; }
    }

    /** 代理转发的 HTTP 状态与响应体。 */
    public static class ProxyResponse {
        private final int status;
        private final String body;

        public ProxyResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public int getStatus() { return status; }
        public String getBody() { return body; }
    }

    /** auth-service ApiResponse 响应封装（含 timestamp，Jackson 反序列化用）。 */
    public static class VerifyEnvelope {
        private int code;
        private String message;
        private VerifyData data;
        private long timestamp;

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public VerifyData getData() { return data; }
        public void setData(VerifyData data) { this.data = data; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    /** /api/auth/verify 的 data 部分。 */
    public static class VerifyData {
        private String subject;
        private String clientId;
        private List<String> roles;
        private long tokenVersion;

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public long getTokenVersion() { return tokenVersion; }
        public void setTokenVersion(long tokenVersion) { this.tokenVersion = tokenVersion; }
    }
}
