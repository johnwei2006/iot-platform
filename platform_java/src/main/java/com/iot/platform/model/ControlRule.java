package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 控制规则（组合 / 定时 / 条件）。
 * <ul>
 *   <li>组合：trigger=MANUAL，devices 为多台同类型设备，手动整组执行；</li>
 *   <li>定时：trigger=TIMED，每天 time 执行 action，可选 closeTime 执行反向动作；</li>
 *   <li>条件：trigger=CONDITION，metricDeviceId 取数，operator 与 threshold 判定，
 *       生效时段 start-end 内触发，可选 useReturn 恢复反向动作。</li>
 * </ul>
 * 启用前必须通过 {@code ConflictChecker} 服务端冲突校验。
 */
@TableName("iot_control_rule")
public class ControlRule {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    private String name;
    /** 组：GROUP / 定时：TIMED / 条件：CONDITION */
    private String kind;
    /** MANUAL / TIMED / CONDITION（trigger 为 MySQL 保留字，需反引号转义） */
    @TableField(value = "`trigger`")
    private String trigger;
    /** 受控设备 JSON 数组：[{"deviceId","nodeId","channel","deviceName","deviceType","action"}] */
    private String devices;
    /** 动作：ON / OFF / OPEN(卷起) / CLOSE(放下) / STOP / LEVEL */
    private String action;
    /** 定时执行时间 HH:mm */
    private String time;
    /** 可选反向动作时间 HH:mm */
    @TableField("close_time")
    private String closeTime;
    /** 条件：指标（temp/humid/soil/water/press） */
    private String metric;
    /** 条件：取数传感器设备编号（station deviceId） */
    @TableField("metric_device_id")
    private String metricDeviceId;
    /** 条件：测点位置（棚口/棚中），展示用 */
    private String station;
    /** 比较符：&gt; / &gt;= / &lt; / &lt;= */
    private String operator;
    private BigDecimal threshold;
    /** 定时规则生效日期 YYYY-MM-DD；条件规则生效时段 HH:mm */
    @TableField("start_time")
    private String startTime;
    /** 定时规则失效日期 YYYY-MM-DD；条件规则生效时段 HH:mm */
    @TableField("end_time")
    private String endTime;
    /** 条件规则生效日期 YYYY-MM-DD（与 HH:mm 时段分开存储） */
    @TableField("effective_from")
    private String effectiveFrom;
    /** 条件规则失效日期 YYYY-MM-DD */
    @TableField("effective_to")
    private String effectiveTo;
    /** 是否启用恢复反向动作 */
    @TableField("use_return")
    private Boolean useReturn;
    private BigDecimal returnThreshold;
    @TableField("plot_id")
    private String plotId;
    @TableField("org_code")
    private String orgCode;
    @TableField("owner_user_id")
    private String ownerUserId;
    /** 是否启用；启用前须通过冲突校验 */
    private Boolean enabled;
    @TableField("created_by")
    private String createdBy;
    @TableField("created_at")
    private Date createdAt;
    @TableField("updated_at")
    private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getTrigger() { return trigger; }
    public void setTrigger(String trigger) { this.trigger = trigger; }
    public String getDevices() { return devices; }
    public void setDevices(String devices) { this.devices = devices; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public String getMetricDeviceId() { return metricDeviceId; }
    public void setMetricDeviceId(String metricDeviceId) { this.metricDeviceId = metricDeviceId; }
    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(String effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public String getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(String effectiveTo) { this.effectiveTo = effectiveTo; }
    public Boolean getUseReturn() { return useReturn; }
    public void setUseReturn(Boolean useReturn) { this.useReturn = useReturn; }
    public BigDecimal getReturnThreshold() { return returnThreshold; }
    public void setReturnThreshold(BigDecimal returnThreshold) { this.returnThreshold = returnThreshold; }
    public String getPlotId() { return plotId; }
    public void setPlotId(String plotId) { this.plotId = plotId; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
