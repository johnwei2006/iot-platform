package com.iot.auth.token;

import java.util.List;

/** JWT 中的自定义 claims，保持最小化：身份、客户端、角色、版本、设备。 */
public class TokenClaims {

    private final String subject;
    private final String clientId;
    private final List<String> roles;
    private final String jti;
    private final long tokenVersion;
    private final String deviceId;

    public TokenClaims(String subject, String clientId, List<String> roles,
                       String jti, long tokenVersion, String deviceId) {
        this.subject = subject;
        this.clientId = clientId;
        this.roles = roles;
        this.jti = jti;
        this.tokenVersion = tokenVersion;
        this.deviceId = deviceId;
    }

    public String getSubject() { return subject; }
    public String getClientId() { return clientId; }
    public List<String> getRoles() { return roles; }
    public String getJti() { return jti; }
    public long getTokenVersion() { return tokenVersion; }
    public String getDeviceId() { return deviceId; }
}
