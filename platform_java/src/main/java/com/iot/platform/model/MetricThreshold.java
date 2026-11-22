package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/** 测点阈值：设备 + 指标 + 测点唯一；超限联动告警。 */
@TableName("iot_metric_threshold")
public class MetricThreshold {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    @TableField("device_id")
    private String deviceId;
    /** 指标键：temp/humid/soil/water/press */
    private String metric;
    /** 测点位置（棚口/棚中），展示用 */
    private String station;
    @TableField("min_value")
    private BigDecimal minValue;
    @TableField("max_value")
    private BigDecimal maxValue;
    @TableField("org_code")
    private String orgCode;
    @TableField("updated_by")
    private String updatedBy;
    @TableField("created_at")
    private Date createdAt;
    @TableField("updated_at")
    private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }
    public BigDecimal getMinValue() { return minValue; }
    public void setMinValue(BigDecimal minValue) { this.minValue = minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}