package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 控制设备（第三层，挂在节点下，按通道区分）。 */
@TableName("iot_device_ctrl")
public class DeviceCtrl {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    private String orgCode;
    /** 控制设备编码 */
    private String deviceCode;
    /** 控制设备名称 */
    private String deviceName;
    /** 控制设备大类编码 */
    private String deviceClass;
    /** 控制设备类型编码 */
    private String deviceClassType;
    /** 节点编码 */
    private String nodeCode;
    /** 状态：0关，1开，2正转，3反转，4暂停，0~100 开度值/任意浮点输出值 */
    private String status;
    /** 是否在线：0-离线，1-在线 */
    private Integer online;
    /** 是否自动：0-手动，1-自动 */
    private Integer auto;
    /** 安装地址 */
    private String address;
    private String longitude;
    private String latitude;
    /** 设备图片地址 */
    private String pic;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    /** 逻辑删除：0-正常，1-删除 */
    private Integer isDeleted;
    /** 端口号 */
    private String channel;
    /** 连接形式 */
    private Integer connectType;
    /** 归属用户（SELF 数据权限） */
    private String ownerUserId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getDeviceClass() { return deviceClass; }
    public void setDeviceClass(String deviceClass) { this.deviceClass = deviceClass; }
    public String getDeviceClassType() { return deviceClassType; }
    public void setDeviceClassType(String deviceClassType) { this.deviceClassType = deviceClassType; }
    public String getNodeCode() { return nodeCode; }
    public void setNodeCode(String nodeCode) { this.nodeCode = nodeCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getOnline() { return online; }
    public void setOnline(Integer online) { this.online = online; }
    public Integer getAuto() { return auto; }
    public void setAuto(Integer auto) { this.auto = auto; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }
    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }
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
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Integer getConnectType() { return connectType; }
    public void setConnectType(Integer connectType) { this.connectType = connectType; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
}
