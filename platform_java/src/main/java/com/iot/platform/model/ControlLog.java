package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 控制执行记录：PENDING → SUCCESS / TIMEOUT / FAILED / PARTIAL。
 * 0x1A 设备控制反馈按 deviceId + nodeId + channel 匹配 PENDING 记录更新结果。
 */
@TableName("iot_control_log")
public class ControlLog {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    /** 触发规则 id（手动下发为空） */
    @TableField("rule_id")
    private String ruleId;
    @TableField("rule_name")
    private String ruleName;
    /** 受控设备编号（station/gateway deviceId） */
    @TableField("device_id")
    private String deviceId;
    @TableField("node_id")
    private String nodeId;
    /** 受控通道（0x1A 回报匹配） */
    private Integer channel;
    @TableField("device_name")
    private String deviceName;
    /** MANUAL / RULE / GROUP */
    private String source;
    @TableField("plot_id")
    private String plotId;
    /** 动作：ON / OFF / OPEN / CLOSE / STOP / LEVEL */
    private String action;
    /** 开度等附加参数 */
    @TableField("action_value")
    private String actionValue;
    /** PENDING / SUCCESS / TIMEOUT / FAILED / PARTIAL */
    private String result;
    /** 设备反馈详情 */
    private String detail;
    @TableField("org_code")
    private String orgCode;
    @TableField("created_at")
    private Date createdAt;
    @TableField("finished_at")
    private Date finishedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public Integer getChannel() { return channel; }
    public void setChannel(Integer channel) { this.channel = channel; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getPlotId() { return plotId; }
    public void setPlotId(String plotId) { this.plotId = plotId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getActionValue() { return actionValue; }
    public void setActionValue(String actionValue) { this.actionValue = actionValue; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Date finishedAt) { this.finishedAt = finishedAt; }
}
