package com.iot.auth.api;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/** 认证 API 的请求/响应 DTO 集中定义（feature-first，避免散落多个小文件）。 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        private String clientId = "web";
        private String deviceId;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }

    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresInMs;
        private UserInfo user;

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        public long getExpiresInMs() { return expiresInMs; }
        public void setExpiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; }
        public UserInfo getUser() { return user; }
        public void setUser(UserInfo user) { this.user = user; }
    }

    /** refresh 请求体可为空：浏览器端 refresh token 由 HttpOnly Cookie 携带。 */
    public static class RefreshRequest {
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class LogoutRequest {
        private String refreshToken;
        private String accessToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    }

    public static class UserInfo {
        private String id;
        private String username;
        private List<String> roles;
        private String orgCode;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public String getOrgCode() { return orgCode; }
        public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    }

    public static class VerifyResponse {
        private String subject;
        private String clientId;
        private List<String> roles;
        private long tokenVersion;
        /** access token 的 jti，供下游本地撤销检查使用 */
        private String jti;
        /** 用户机构编码（4-2-2-2 层级前缀） */
        private String org;
        /** 数据权限映射：业务实体编码 -> ALL|ORG_AND_CHILDREN|ORG_ONLY|SELF */
        private Map<String, String> dataScopes;

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public long getTokenVersion() { return tokenVersion; }
        public void setTokenVersion(long tokenVersion) { this.tokenVersion = tokenVersion; }
        public String getJti() { return jti; }
        public void setJti(String jti) { this.jti = jti; }
        public String getOrg() { return org; }
        public void setOrg(String org) { this.org = org; }
        public Map<String, String> getDataScopes() { return dataScopes; }
        public void setDataScopes(Map<String, String> dataScopes) { this.dataScopes = dataScopes; }
    }
}
