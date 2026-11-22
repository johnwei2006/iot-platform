package com.iot.admin.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 机构已分配产品，唯一键 product_id + org_id。 */
@TableName("sys_product_org")
public class ProductOrg {

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("product_id")
    private String productId;

    @TableField("org_id")
    private String orgId;

    @TableField("created_at")
    private Date createdAt = new Date();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
