package com.iot.admin.org;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 机构：编码由 4-2-2-2 数字构成（一级 4 位、逐级 +2 位，最深 4 级），
 * 编码前缀天然表达层级（如 00010102 属于 0001 → 000101）。
 */
@Document(collection = "orgs")
public class Org {

    @Id
    private String id;

    /** 机构编码（4/6/8/10 位数字），层级前缀 */
    @Indexed(unique = true)
    private String code;

    private String name;

    /** 上级机构编码（一级机构为 null） */
    @Indexed(sparse = true)
    private String parentCode;

    private String leader;
    private String phone;
    private String sort = "0";
    private String status = "ENABLED";
    private Date createdAt = new Date();

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
