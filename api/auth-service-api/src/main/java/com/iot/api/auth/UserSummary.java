package com.iot.api.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Internal user summary, JSON-compatible with auth-service InternalUserService.UserSummary. */
public class UserSummary {
    private String id;
    private String username;
    private List<String> roles;
    private String status;
    private long tokenVersion;
    private String orgId;
    private String orgCode;
    private Map<String, String> dataScopes = new HashMap<String, String>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(long tokenVersion) { this.tokenVersion = tokenVersion; }
    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public Map<String, String> getDataScopes() { return dataScopes; }
    public void setDataScopes(Map<String, String> dataScopes) { this.dataScopes = dataScopes; }
}
