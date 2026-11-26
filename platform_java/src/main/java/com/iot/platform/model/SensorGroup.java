package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 传感器组合：图表/表格数据页的测点分组。 */
@TableName("iot_sensor_group")
public class SensorGroup {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String orgCode;
    private String groupName;
    /** 传感器编码列表（JSON 数组字符串） */
    private String sensorCodes;
    private String description;
    private String createBy;
    private java.util.Date createTime;
    private String updateBy;
    private java.util.Date updateTime;
    private Integer isDeleted;
    private String ownerUserId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getSensorCodes() { return sensorCodes; }
    public void setSensorCodes(String sensorCodes) { this.sensorCodes = sensorCodes; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
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
