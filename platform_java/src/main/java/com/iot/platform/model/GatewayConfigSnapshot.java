package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 网关配置信息（含协议配置字段、上报快照与下发状态）。 */
@TableName("iot_gateway_config")
public class GatewayConfigSnapshot {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FAILED = "FAILED";

    @TableId(value = "id", type = com.baomidou.mybatisplus.annotation.IdType.NONE)
    private String id;
    @TableField("gateway_code")
    private String gatewayCode;
    @TableField("service_type")
    private Integer serviceType;
    private String category;
    private String payload;
    @TableField("reported_at")
    private Date reportedAt;
    @TableField("created_at")
    private Date createdAt;

    /** 版本号 */
    @TableField("config_version")
    private Integer configVersion;
    /** 网关编号 */
    @TableField("gateway_id")
    private String gatewayId;
    /** 项目编号 */
    @TableField("project_id")
    private String projectId;
    /** 系统服务器地址 */
    @TableField("system_server_url")
    private String systemServerUrl;
    /** 服务器端口 */
    @TableField("system_server_port")
    private Integer systemServerPort;
    /** 心跳间隔 */
    @TableField("heartbeat_interval")
    private Integer heartbeatInterval;
    /** 上报间隔 */
    @TableField("report_interval")
    private Integer reportInterval;
    /** Lora频率组 */
    @TableField("lora_freq_group")
    private Integer loraFreqGroup;
    /** 配置时间 */
    @TableField("config_time")
    private Date configTime;
    /** 下发时间 */
    @TableField("send_time")
    private Date sendTime;
    /** 状态：SUCCESS/PENDING/FAILED */
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGatewayCode() { return gatewayCode; }
    public void setGatewayCode(String gatewayCode) { this.gatewayCode = gatewayCode; }
    public Integer getServiceType() { return serviceType; }
    public void setServiceType(Integer serviceType) { this.serviceType = serviceType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Date getReportedAt() { return reportedAt; }
    public void setReportedAt(Date reportedAt) { this.reportedAt = reportedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Integer getConfigVersion() { return configVersion; }
    public void setConfigVersion(Integer configVersion) { this.configVersion = configVersion; }
    public String getGatewayId() { return gatewayId; }
    public void setGatewayId(String gatewayId) { this.gatewayId = gatewayId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getSystemServerUrl() { return systemServerUrl; }
    public void setSystemServerUrl(String systemServerUrl) { this.systemServerUrl = systemServerUrl; }
    public Integer getSystemServerPort() { return systemServerPort; }
    public void setSystemServerPort(Integer systemServerPort) { this.systemServerPort = systemServerPort; }
    public Integer getHeartbeatInterval() { return heartbeatInterval; }
    public void setHeartbeatInterval(Integer heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }
    public Integer getReportInterval() { return reportInterval; }
    public void setReportInterval(Integer reportInterval) { this.reportInterval = reportInterval; }
    public Integer getLoraFreqGroup() { return loraFreqGroup; }
    public void setLoraFreqGroup(Integer loraFreqGroup) { this.loraFreqGroup = loraFreqGroup; }
    public Date getConfigTime() { return configTime; }
    public void setConfigTime(Date configTime) { this.configTime = configTime; }
    public Date getSendTime() { return sendTime; }
    public void setSendTime(Date sendTime) { this.sendTime = sendTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
