package com.iot.admin.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 产品主数据：平台功能域（系统管理 / 大屏 / 物联网 / 农事管理，可扩展）。 */
@TableName("sys_product")
public class Product {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    private String code;
    private String name;
    private String description;
    private String sort = "0";
    private String status = "ENABLED";

    @TableField("built_in")
    private boolean builtIn;

    @TableField("created_at")
    private Date createdAt = new Date();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isBuiltIn() { return builtIn; }
    public void setBuiltIn(boolean builtIn) { this.builtIn = builtIn; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
