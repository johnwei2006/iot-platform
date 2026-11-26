package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 节点（第二层，挂在网关下）。 */
@TableName("iot_node")
public class Node {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    /** 所属机构编码（数据权限过滤依据） */
    private String orgCode;
    /** 节点编码 */
    private String nodeCode;
    /** 节点名称 */
    private String nodeName;
    /** 网关编码（挂靠 iot_gateway.gateway_code） */
    private String gatewayCode;
    /** 状态（无用） */
    private Integer status;
    /** 安装区域ID（iot_area.id） */
    private String areaId;
    /** 安装地点 */
    private String address;
    /** 经度 */
    private String longitude;
    /** 纬度 */
    private String latitude;
    /** 节点类型：1-ACT，2-IRRI，3-DATA，4-植保机，5-开度 */
    private Integer nodeType;
    /** 固件版本号 */
    private String firmwareVersion;
    /** 信号质量 */
    private String rssi;
    /** 信噪比 */
    private String snr;
    /** 速率 */
    private String speed;
    /** 电池电压 */
    private String batteryVoltage;
    /** 上报频率 */
    private Integer reportInterval;
    /** 最后上报时间 */
    private Date lastReportTime;
    /** 是否在线：0-离线，1-在线 */
    private Integer online;
    /** 设备图片地址 */
    private String pic;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    /** 逻辑删除：0-正常，1-删除 */
    private Integer isDeleted;
    /** 归属用户（SELF 数据权限） */
    private String ownerUserId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public String getNodeCode() { return nodeCode; }
    public void setNodeCode(String nodeCode) { this.nodeCode = nodeCode; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getGatewayCode() { return gatewayCode; }
    public void setGatewayCode(String gatewayCode) { this.gatewayCode = gatewayCode; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getAreaId() { return areaId; }
    public void setAreaId(String areaId) { this.areaId = areaId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }
    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }
    public Integer getNodeType() { return nodeType; }
    public void setNodeType(Integer nodeType) { this.nodeType = nodeType; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    public String getRssi() { return rssi; }
    public void setRssi(String rssi) { this.rssi = rssi; }
    public String getSnr() { return snr; }
    public void setSnr(String snr) { this.snr = snr; }
    public String getSpeed() { return speed; }
    public void setSpeed(String speed) { this.speed = speed; }
    public String getBatteryVoltage() { return batteryVoltage; }
    public void setBatteryVoltage(String batteryVoltage) { this.batteryVoltage = batteryVoltage; }
    public Integer getReportInterval() { return reportInterval; }
    public void setReportInterval(Integer reportInterval) { this.reportInterval = reportInterval; }
    public Date getLastReportTime() { return lastReportTime; }
    public void setLastReportTime(Date lastReportTime) { this.lastReportTime = lastReportTime; }
    public Integer getOnline() { return online; }
    public void setOnline(Integer online) { this.online = online; }
    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }
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
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
}
