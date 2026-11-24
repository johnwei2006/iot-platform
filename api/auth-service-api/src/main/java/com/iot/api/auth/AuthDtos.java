package com.iot.api.auth;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/** Shared auth API request/response DTOs. Must stay JSON-compatible with auth-service endpoints. */
public final class AuthDtos {

    private AuthDtos() {}

    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
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

    /** Response of POST /api/auth/verify. */
    public static class VerifyResponse {
        private String subject;
        private String clientId;
        private List<String> roles;
        private long tokenVersion;
        private String jti;
        private String org;
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
