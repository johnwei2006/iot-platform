package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 网关配置下发命令日志。 */
@TableName("iot_gateway_command_log")
public class GatewayCommandLog {

    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String TIMEOUT = "TIMEOUT";

    @TableId(value = "id", type = com.baomidou.mybatisplus.annotation.IdType.NONE)
    private String id;
    @TableField("gateway_code")
    private String gatewayCode;
    @TableField("service_type")
    private Integer serviceType;
    @TableField("command_type")
    private String commandType;
    @TableField("request_id")
    private String requestId;
    private String payload;
    private String status;
    private String result;
    private String reason;
    @TableField("sent_at")
    private Date sentAt;
    @TableField("finished_at")
    private Date finishedAt;
    @TableField("created_by")
    private String createdBy;
    @TableField("created_at")
    private Date createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGatewayCode() { return gatewayCode; }
    public void setGatewayCode(String gatewayCode) { this.gatewayCode = gatewayCode; }
    public Integer getServiceType() { return serviceType; }
    public void setServiceType(Integer serviceType) { this.serviceType = serviceType; }
    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }
    public Date getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Date finishedAt) { this.finishedAt = finishedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
