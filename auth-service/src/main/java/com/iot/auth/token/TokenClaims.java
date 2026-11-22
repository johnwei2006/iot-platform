package com.iot.auth.token;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JWT 中的自定义 claims，保持最小化：身份、客户端、角色、版本、设备、机构、数据权限。
 * ds 为业务实体编码 -> ALL|ORG_AND_CHILDREN|ORG_ONLY|SELF 的映射（由 admin-service 合并计算）。
 */
public class TokenClaims {

    private final String subject;
    private final String clientId;
    private final List<String> roles;
    private final String jti;
    private final long tokenVersion;
    private final String deviceId;
    /** 用户所属机构编码（4-2-2-2 数字层级前缀） */
    private final String orgCode;
    /** 数据权限映射：实体编码 -> 范围 */
    private final Map<String, String> dataScopes;

    public TokenClaims(String subject, String clientId, List<String> roles,
                       String jti, long tokenVersion, String deviceId) {
        this(subject, clientId, roles, jti, tokenVersion, deviceId, null, null);
    }

    public TokenClaims(String subject, String clientId, List<String> roles,
                       String jti, long tokenVersion, String deviceId,
                       String orgCode, Map<String, String> dataScopes) {
        this.subject = subject;
        this.clientId = clientId;
        this.roles = roles;
        this.jti = jti;
        this.tokenVersion = tokenVersion;
        this.deviceId = deviceId;
        this.orgCode = orgCode;
        this.dataScopes = dataScopes == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(dataScopes);
    }

    public String getSubject() { return subject; }
    public String getClientId() { return clientId; }
    public List<String> getRoles() { return roles; }
    public String getJti() { return jti; }
    public long getTokenVersion() { return tokenVersion; }
    public String getDeviceId() { return deviceId; }
    public String getOrgCode() { return orgCode; }
    public Map<String, String> getDataScopes() { return dataScopes; }
}
