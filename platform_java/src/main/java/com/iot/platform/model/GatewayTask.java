package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 网关本地任务定义（0x25 任务列表配置）。 */
@TableName("iot_gateway_task")
public class GatewayTask {

    @TableId(value = "id", type = com.baomidou.mybatisplus.annotation.IdType.NONE)
    private String id;
    @TableField("gateway_code")
    private String gatewayCode;
    @TableField("task_name")
    private String taskName;
    @TableField("task_no")
    private Integer taskNo;
    private Boolean enabled;
    @TableField("trigger_type")
    private String triggerType;
    @TableField("trigger_time")
    private String triggerTime;
    private String triggers;
    private String actions;
    @TableField("deploy_status")
    private String deployStatus;
    @TableField("feedback_msg")
    private String feedbackMsg;
    @TableField("org_code")
    private String orgCode;
    @TableField("owner_user_id")
    private String ownerUserId;
    @TableField("created_at")
    private Date createdAt;
    @TableField("updated_at")
    private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGatewayCode() { return gatewayCode; }
    public void setGatewayCode(String gatewayCode) { this.gatewayCode = gatewayCode; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public Integer getTaskNo() { return taskNo; }
    public void setTaskNo(Integer taskNo) { this.taskNo = taskNo; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getTriggerTime() { return triggerTime; }
    public void setTriggerTime(String triggerTime) { this.triggerTime = triggerTime; }
    public String getTriggers() { return triggers; }
    public void setTriggers(String triggers) { this.triggers = triggers; }
    public String getActions() { return actions; }
    public void setActions(String actions) { this.actions = actions; }
    public String getDeployStatus() { return deployStatus; }
    public void setDeployStatus(String deployStatus) { this.deployStatus = deployStatus; }
    public String getFeedbackMsg() { return feedbackMsg; }
    public void setFeedbackMsg(String feedbackMsg) { this.feedbackMsg = feedbackMsg; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
