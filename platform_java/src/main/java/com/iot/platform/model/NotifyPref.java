package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 通知偏好：接收类型与渠道开关（本期仅存配置，不实际推送）。 */
@TableName("iot_notify_pref")
public class NotifyPref {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    @TableField("user_id")
    private String userId;
    /** 接收类型开关 JSON：["TASK","ALARM","SYSTEM"] */
    @TableField("receive_kinds")
    private String receiveKinds;
    /** 渠道开关 JSON：["INAPP","WECHAT","SMS"]，本期仅存 */
    private String channels;
    @TableField("updated_at")
    private Date updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getReceiveKinds() { return receiveKinds; }
    public void setReceiveKinds(String receiveKinds) { this.receiveKinds = receiveKinds; }
    public String getChannels() { return channels; }
    public void setChannels(String channels) { this.channels = channels; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}