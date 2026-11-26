package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/** 区域管理：卫星地图上的多边形地块。 */
@TableName("iot_area")
public class Area {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String orgCode;
    /** 区域名称 */
    private String areaName;
    /** 多边形边界（JSON 数组字符串，[[lng,lat],...]） */
    private String boundary;
    /** 绘制面积（亩，由边界自动计算） */
    private BigDecimal drawAreaMu;
    /** 标注面积（亩，用户填写） */
    private BigDecimal labelAreaMu;
    private String createBy;
    private java.util.Date createTime;
    private String updateBy;
    private java.util.Date updateTime;
    private Integer isDeleted;
    /** 属主（SELF 数据权限） */
    private String ownerUserId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }
    public String getBoundary() { return boundary; }
    public void setBoundary(String boundary) { this.boundary = boundary; }
    public BigDecimal getDrawAreaMu() { return drawAreaMu; }
    public void setDrawAreaMu(BigDecimal drawAreaMu) { this.drawAreaMu = drawAreaMu; }
    public BigDecimal getLabelAreaMu() { return labelAreaMu; }
    public void setLabelAreaMu(BigDecimal labelAreaMu) { this.labelAreaMu = labelAreaMu; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public java.util.Date getCreateTime() { return createTime; }
    public void setCreateTime(java.util.Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public java.util.Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.util.Date updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
}
