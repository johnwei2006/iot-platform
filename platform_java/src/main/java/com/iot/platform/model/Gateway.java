package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName("iot_gateway")
public class Gateway {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    /** 网关编码（原 device_id；Java 字段名保留以兼容调用方与前端） */
    @TableField("gateway_code")
    private String deviceId;
    private String nodeId;
    private String name;
    /** 标识类型：VIN / TERMINALNO / DEVICEID */
    private String identifierType;
    /** 网关认证预共享密钥 */
    private String authSecret;
    /** 状态：ONLINE / OFFLINE */
    private String status;
    /** 所属机构编码（4-2-2-2 层级前缀，数据权限过滤依据） */
    private String orgCode;
    /** 归属用户（SELF 数据权限依据，通常为创建者） */
    @TableField("owner_user_id")
    private String ownerUserId;
    @TableField("last_seen")
    private Date lastSeen;
    @TableField("created_at")
    private Date createdAt;
    @TableField("updated_at")
    private Date updatedAt;
    /** 安装地点 */
    private String address;
    /** 经度 */
    private String longitude;
    /** 纬度 */
    private String latitude;
    /** 网关类型：1-4G网关，2-5G网关 ... */
    private Integer gatewayType;
    /** 固件版本号 */
    @TableField("firmware_version")
    private String firmwareVersion;
    /** 电池电压 */
    private String power;
    /** 设备图片地址 */
    private String pic;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIdentifierType() { return identifierType; }
    public void setIdentifierType(String identifierType) { this.identifierType = identifierType; }
    public String getAuthSecret() { return authSecret; }
    public void setAuthSecret(String authSecret) { this.authSecret = authSecret; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    public Date getLastSeen() { return lastSeen; }
    public void setLastSeen(Date lastSeen) { this.lastSeen = lastSeen; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }
    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }
    public Integer getGatewayType() { return gatewayType; }
    public void setGatewayType(Integer gatewayType) { this.gatewayType = gatewayType; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    public String getPower() { return power; }
    public void setPower(String power) { this.power = power; }
    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }
}
