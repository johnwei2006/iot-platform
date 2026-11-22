package com.iot.auth.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/** 认证审计事件（MongoDB auth_events 集合）。 */
@Document(collection = "auth_events")
public class AuthEvent {

    @Id
    private String id;

    /** LOGIN_SUCCESS / LOGIN_FAILED / RATE_LIMITED / LOGOUT / REVOKE_ALL / REFRESH_REUSE / SSO_EXCHANGE */
    private String type;
    private String userId;
    private String username;
    private String clientId;
    private String ip;
    private String detail;
    private Date at = new Date();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Date getAt() { return at; }
    public void setAt(Date at) { this.at = at; }
}
