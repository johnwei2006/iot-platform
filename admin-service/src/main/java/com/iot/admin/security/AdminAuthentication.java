package com.iot.admin.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** JWT 验签后的管理员身份（含机构与数据权限声明）。 */
public class AdminAuthentication {

    private final String subject;
    private final String clientId;
    private final List<String> roles;
    private final long tokenVersion;
    private final String jti;
    private final String orgCode;
    private final Map<String, String> dataScopes;

    public AdminAuthentication(String subject, String clientId, List<String> roles,
                               long tokenVersion, String jti, String orgCode,
                               Map<String, String> dataScopes) {
        this.subject = subject;
        this.clientId = clientId;
        this.roles = roles;
        this.tokenVersion = tokenVersion;
        this.jti = jti;
        this.orgCode = orgCode;
        this.dataScopes = dataScopes == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(dataScopes);
    }

    public String getSubject() { return subject; }
    public String getClientId() { return clientId; }
    public List<String> getRoles() { return roles; }
    public long getTokenVersion() { return tokenVersion; }
    public String getJti() { return jti; }
    public String getOrgCode() { return orgCode; }
    public Map<String, String> getDataScopes() { return dataScopes; }

    /** 是否为内置超级管理员。 */
    public boolean isSuperAdmin() {
        return roles != null && roles.contains("SUPER_ADMIN");
    }
}
