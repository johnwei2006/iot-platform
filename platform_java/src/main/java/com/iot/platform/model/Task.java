package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 站内任务：分配、完成说明、超时态标记（超时=完成时间晚于截止，或未完成且已过期）。 */
@TableName("iot_task")
public class Task {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    private String title;
    private String content;
    @TableField("assignee_user_id")
    private String assigneeUserId;
    @TableField("due_time")
    private Date dueTime;
    /** TODO / DONE */
    private String status;
    @TableField("done_time")
    private Date doneTime;
    @TableField("done_note")
    private String doneNote;
    @TableField("org_code")
    private String orgCode;
    @TableField("created_by")
    private String createdBy;
    @TableField("created_at")
    private Date createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAssigneeUserId() { return assigneeUserId; }
    public void setAssigneeUserId(String assigneeUserId) { this.assigneeUserId = assigneeUserId; }
    public Date getDueTime() { return dueTime; }
    public void setDueTime(Date dueTime) { this.dueTime = dueTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getDoneTime() { return doneTime; }
    public void setDoneTime(Date doneTime) { this.doneTime = doneTime; }
    public String getDoneNote() { return doneNote; }
    public void setDoneNote(String doneNote) { this.doneNote = doneNote; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}