package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 传感器（第三层，挂在节点下，按通道区分）。 */
@TableName("iot_sensor")
public class Sensor {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    private String orgCode;
    /** 传感器编码 */
    private String sensorCode;
    /** 传感器名称 */
    private String sensorName;
    /** 节点编码 */
    private String nodeCode;
    /** 传感器大类编码 */
    private String sensorClass;
    /** 传感器类型编码 */
    private String sensorClassType;
    /** 状态：0-正常，1-异常 */
    private Integer status;
    /** 单位 */
    private String unit;
    /** 最小预警值（传感器级阈值，优先于网关级 MetricThreshold） */
    private Double warnMin;
    /** 最大预警值 */
    private Double warnMax;
    /** 小数位 */
    private Integer scale;
    /** 上报频率 */
    private Integer reportInterval;
    /** 最后上报时间 */
    private Date lastReportTime;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    /** 逻辑删除：0-正常，1-删除 */
    private Integer isDeleted;
    /** 图片地址 */
    private String pic;
    /** 端口号：Data 节点实际端口；4G 网关为虚拟端口 */
    private String channel;
    /** 读数指令（485 Hex，接入虚拟节点下 485 传感器时非空） */
    private String commandRead;
    /** 归属用户（SELF 数据权限） */
    private String ownerUserId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public String getSensorCode() { return sensorCode; }
    public void setSensorCode(String sensorCode) { this.sensorCode = sensorCode; }
    public String getSensorName() { return sensorName; }
    public void setSensorName(String sensorName) { this.sensorName = sensorName; }
    public String getNodeCode() { return nodeCode; }
    public void setNodeCode(String nodeCode) { this.nodeCode = nodeCode; }
    public String getSensorClass() { return sensorClass; }
    public void setSensorClass(String sensorClass) { this.sensorClass = sensorClass; }
    public String getSensorClassType() { return sensorClassType; }
    public void setSensorClassType(String sensorClassType) { this.sensorClassType = sensorClassType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Double getWarnMin() { return warnMin; }
    public void setWarnMin(Double warnMin) { this.warnMin = warnMin; }
    public Double getWarnMax() { return warnMax; }
    public void setWarnMax(Double warnMax) { this.warnMax = warnMax; }
    public Integer getScale() { return scale; }
    public void setScale(Integer scale) { this.scale = scale; }
    public Integer getReportInterval() { return reportInterval; }
    public void setReportInterval(Integer reportInterval) { this.reportInterval = reportInterval; }
    public Date getLastReportTime() { return lastReportTime; }
    public void setLastReportTime(Date lastReportTime) { this.lastReportTime = lastReportTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getCommandRead() { return commandRead; }
    public void setCommandRead(String commandRead) { this.commandRead = commandRead; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
}
