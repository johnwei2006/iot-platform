package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName("iot_alarm")
public class Alarm {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    @TableField("device_id")
    private String deviceId;
    private String rule;
    /** 级别：INFO / WARN / ERROR */
    private String level;
    private String message;
    /** 状态：OPEN / RESOLVED */
    private String status;
    @TableField("created_at")
    private Date createdAt;
    @TableField("resolved_at")
    private Date resolvedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Date resolvedAt) { this.resolvedAt = resolvedAt; }
}
