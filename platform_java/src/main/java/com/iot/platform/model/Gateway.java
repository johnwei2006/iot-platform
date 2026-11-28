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
    /** 安装区域ID（iot_area.id） */
    private String areaId;
    /** 安装地点 */
    private String address;
    /** 经度 */
    private String longitude;
    /** 纬度 */
    private String latitude;
    /** 网关类型：1-4G，2-工控网关，3-5G，4-其它 */
    private Integer gatewayType;
    /** 固件版本号 */
    @TableField("firmware_version")
    private String firmwareVersion;
    /** 电池电压 */
    private String power;
    /** 设备图片地址 */
    private String pic;

    /** 0x06 网关信息上报字段：配置版本号 */
    @TableField("config_version")
    private Integer configVersion;
    /** 0x06 上报网关编号（区别于平台内部 gateway_code） */
    @TableField("gateway_id")
    private String gatewayId;
    @TableField("project_id")
    private String projectId;
    @TableField("heartbeat_interval")
    private Integer heartbeatInterval;
    @TableField("report_interval")
    private Integer reportInterval;
    @TableField("rtc_time")
    private String rtcTime;
    @TableField("system_server_url")
    private String systemServerUrl;
    @TableField("system_server_port")
    private Integer systemServerPort;
    @TableField("hardware_version")
    private String hardwareVersion;
    @TableField("gateway_mcuid")
    private String gatewayMcuid;
    @TableField("memory_total")
    private Integer memoryTotal;
    @TableField("memory_used")
    private Integer memoryUsed;
    @TableField("flash_total")
    private Integer flashTotal;
    @TableField("flash_used")
    private Integer flashUsed;
    @TableField("uplink_message_size")
    private Integer uplinkMessageSize;
    @TableField("downlink_message_size")
    private Integer downlinkMessageSize;
    /** 0x06 上报电池电压（与历史 power 字段分开保存） */
    @TableField("battery")
    private String battery;
    @TableField("adapter_exists")
    private Integer adapterExists;
    @TableField("charging_enabled")
    private Integer chargingEnabled;
    @TableField("latest_reboot_time")
    private String latestRebootTime;
    /** 最近一次网关信息上报时间 */
    @TableField("info_reported_at")
    private Date infoReportedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
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
    public String getAreaId() { return areaId; }
    public void setAreaId(String areaId) { this.areaId = areaId; }
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
    public Integer getConfigVersion() { return configVersion; }
    public void setConfigVersion(Integer configVersion) { this.configVersion = configVersion; }
    public String getGatewayId() { return gatewayId; }
    public void setGatewayId(String gatewayId) { this.gatewayId = gatewayId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public Integer getHeartbeatInterval() { return heartbeatInterval; }
    public void setHeartbeatInterval(Integer heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }
    public Integer getReportInterval() { return reportInterval; }
    public void setReportInterval(Integer reportInterval) { this.reportInterval = reportInterval; }
    public String getRtcTime() { return rtcTime; }
    public void setRtcTime(String rtcTime) { this.rtcTime = rtcTime; }
    public String getSystemServerUrl() { return systemServerUrl; }
    public void setSystemServerUrl(String systemServerUrl) { this.systemServerUrl = systemServerUrl; }
    public Integer getSystemServerPort() { return systemServerPort; }
    public void setSystemServerPort(Integer systemServerPort) { this.systemServerPort = systemServerPort; }
    public String getHardwareVersion() { return hardwareVersion; }
    public void setHardwareVersion(String hardwareVersion) { this.hardwareVersion = hardwareVersion; }
    public String getGatewayMcuid() { return gatewayMcuid; }
    public void setGatewayMcuid(String gatewayMcuid) { this.gatewayMcuid = gatewayMcuid; }
    public Integer getMemoryTotal() { return memoryTotal; }
    public void setMemoryTotal(Integer memoryTotal) { this.memoryTotal = memoryTotal; }
    public Integer getMemoryUsed() { return memoryUsed; }
    public void setMemoryUsed(Integer memoryUsed) { this.memoryUsed = memoryUsed; }
    public Integer getFlashTotal() { return flashTotal; }
    public void setFlashTotal(Integer flashTotal) { this.flashTotal = flashTotal; }
    public Integer getFlashUsed() { return flashUsed; }
    public void setFlashUsed(Integer flashUsed) { this.flashUsed = flashUsed; }
    public Integer getUplinkMessageSize() { return uplinkMessageSize; }
    public void setUplinkMessageSize(Integer uplinkMessageSize) { this.uplinkMessageSize = uplinkMessageSize; }
    public Integer getDownlinkMessageSize() { return downlinkMessageSize; }
    public void setDownlinkMessageSize(Integer downlinkMessageSize) { this.downlinkMessageSize = downlinkMessageSize; }
    public String getBattery() { return battery; }
    public void setBattery(String battery) { this.battery = battery; }
    public Integer getAdapterExists() { return adapterExists; }
    public void setAdapterExists(Integer adapterExists) { this.adapterExists = adapterExists; }
    public Integer getChargingEnabled() { return chargingEnabled; }
    public void setChargingEnabled(Integer chargingEnabled) { this.chargingEnabled = chargingEnabled; }
    public String getLatestRebootTime() { return latestRebootTime; }
    public void setLatestRebootTime(String latestRebootTime) { this.latestRebootTime = latestRebootTime; }
    public Date getInfoReportedAt() { return infoReportedAt; }
    public void setInfoReportedAt(Date infoReportedAt) { this.infoReportedAt = infoReportedAt; }
}
