package com.iot.auth.identity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iot.auth.common.JsonTypeHandlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一身份用户；口令只存 BCrypt 哈希，角色使用大写枚举值。
 * 持久化到 MySQL sys_user，审计/SSO/refresh 仍在 MongoDB/Redis。
 */
@TableName(value = "sys_user", autoResultMap = true)
public class User {

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    private String username;

    /** BCrypt 哈希，禁止明文 */
    @TableField("password_hash")
    private String passwordHash;

    private String openid;
    private String unionid;
    private String phone;

    @TableField(value = "roles", typeHandler = JsonTypeHandlers.ListStringTypeHandler.class)
    private List<String> roles = new ArrayList<String>();

    /** 所属机构（admin-service sys_org 主键） */
    @TableField("org_id")
    private String orgId;

    /** 所属机构编码（4-2-2-2 数字，层级前缀） */
    @TableField("org_code")
    private String orgCode;

    /** 数据权限映射：业务实体编码 -> ALL|ORG_AND_CHILDREN|ORG_ONLY|SELF */
    @TableField(value = "data_scopes", typeHandler = JsonTypeHandlers.MapStringTypeHandler.class)
    private Map<String, String> dataScopes = new HashMap<String, String>();

    @TableField("token_version")
    private long tokenVersion = 0L;

    private String status = "ENABLED";

    @TableField("created_at")
    private Date createdAt = new Date();

    @TableField("last_login_at")
    private Date lastLoginAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getUnionid() { return unionid; }
    public void setUnionid(String unionid) { this.unionid = unionid; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles == null ? new ArrayList<String>() : roles; }
    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public Map<String, String> getDataScopes() { return dataScopes; }
    public void setDataScopes(Map<String, String> dataScopes) {
        this.dataScopes = dataScopes == null ? new HashMap<String, String>() : dataScopes;
    }
    public long getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(long tokenVersion) { this.tokenVersion = tokenVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public boolean isEnabled() {
        return "ENABLED".equals(status);
    }
}
