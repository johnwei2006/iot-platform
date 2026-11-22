package com.iot.admin.org;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;
import java.util.UUID;

/**
 * 机构：编码由 4-2-2-2 数字构成（一级 4 位、逐级 +2 位，最深 4 级），
 * 编码前缀天然表达层级（如 00010102 属于 0001 → 000101）。
 *
 * <p>持久化到 MySQL sys_org；历史 Mongo 数据迁移时保留原始 id。
 */
@Entity
@Table(name = "sys_org", indexes = {
        @Index(name = "idx_sys_org_parent_code", columnList = "parent_code")
})
public class Org {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    /** 机构编码（4/6/8/10 位数字），层级前缀 */
    @Column(name = "code", length = 32, nullable = false, unique = true)
    private String code;

    private String name;

    /** 上级机构编码（一级机构为 null） */
    @Column(name = "parent_code", length = 32)
    private String parentCode;

    private String leader;
    private String phone;

    @Column(name = "sort", length = 16)
    private String sort = "0";

    @Column(name = "status", length = 16)
    private String status = "ENABLED";

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @PrePersist
    void ensureId() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getParentCode() { return parentCode; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }
    public String getLeader() { return leader; }
    public void setLeader(String leader) { this.leader = leader; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
