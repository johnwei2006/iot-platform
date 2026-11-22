package com.iot.auth.identity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 统一身份用户；口令只存 BCrypt 哈希，角色使用大写枚举值。 */
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String username;

    /** BCrypt 哈希，禁止明文 */
    private String passwordHash;

    @Indexed(unique = true, sparse = true)
    private String openid;

    @Indexed(unique = true, sparse = true)
    private String unionid;

    @Indexed(unique = true, sparse = true)
    private String phone;

    private List<String> roles = new ArrayList<String>();

    /** 所属机构（admin-service 机构文档 id） */
    private String orgId;

    /** 所属机构编码（4-2-2-2 数字，层级前缀），冗余存储避免跨服务查树 */
    @Indexed(sparse = true)
    private String orgCode;

    /** 数据权限映射：业务实体编码 -> ALL|ORG_AND_CHILDREN|ORG_ONLY|SELF（由 admin-service 合并计算后同步） */
    private Map<String, String> dataScopes = new HashMap<String, String>();

    /** 递增此值可让该用户所有历史 token 失效 */
    private long tokenVersion = 0L;

    /** ENABLED / DISABLED */
    private String status = "ENABLED";

    private Date createdAt = new Date();
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
