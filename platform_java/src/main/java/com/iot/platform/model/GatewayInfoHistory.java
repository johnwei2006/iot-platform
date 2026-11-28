package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 0x06 网关上报历史信息记录。 */
@TableName("iot_gateway_info")
public class GatewayInfoHistory {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    @TableField("gateway_code")
    private String gatewayCode;
    @TableField("config_version")
    private Integer configVersion;
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
    @TableField("firmware_version")
    private String firmwareVersion;
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
    private String battery;
    @TableField("adapter_exists")
    private Integer adapterExists;
    @TableField("charging_enabled")
    private Integer chargingEnabled;
    @TableField("latest_reboot_time")
    private String latestRebootTime;
    private String payload;
    @TableField("reported_at")
    private Date reportedAt;
    @TableField("created_at")
    private Date createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGatewayCode() { return gatewayCode; }
    public void setGatewayCode(String gatewayCode) { this.gatewayCode = gatewayCode; }
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
    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
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
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Date getReportedAt() { return reportedAt; }
    public void setReportedAt(Date reportedAt) { this.reportedAt = reportedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
