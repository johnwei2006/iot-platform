package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 站内消息：任务/告警/系统通知，read_at 为空表示未读（角标）。本期仅站内，不实际推送。 */
@TableName("iot_message")
public class Message {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    @TableField("user_id")
    private String userId;
    /** ALARM / SYSTEM */
    private String type;
    /** 关联对象 id（告警/控制记录） */
    @TableField("ref_id")
    private String refId;
    private String title;
    private String content;
    @TableField("read_at")
    private Date readAt;
    @TableField("created_at")
    private Date createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Date getReadAt() { return readAt; }
    public void setReadAt(Date readAt) { this.readAt = readAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}