package com.iot.admin.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 产品包含的菜单范围（含按钮），唯一键 product_id + menu_id。 */
@TableName("sys_product_menu")
public class ProductMenu {

    @TableId(value = "id", type = IdType.NONE)
    private String id;

    @TableField("product_id")
    private String productId;

    @TableField("menu_id")
    private String menuId;

    @TableField("created_at")
    private Date createdAt = new Date();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getMenuId() { return menuId; }
    public void setMenuId(String menuId) { this.menuId = menuId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
