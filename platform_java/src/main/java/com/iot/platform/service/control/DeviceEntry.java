package com.iot.platform.service.control;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** 规则 devices JSON 数组元素：受控设备引用。 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceEntry {

    /** 设备编号（station/gateway deviceId，下行指令的目标网关） */
    private String deviceId;
    /** 执行节点（0x19 报文 nodeId） */
    private String nodeId;
    /** 执行通道 */
    private Integer channel;
    private String deviceName;
    /** 设备类型：风机 / 棉被 等，组合要求同类型 */
    private String deviceType;
    /** 默认动作（组合规则展示用）：ON / OFF */
    private String action;

    public DeviceEntry() {
    }

    public DeviceEntry(String deviceId, String nodeId, Integer channel, String deviceName, String deviceType) {
        this.deviceId = deviceId;
        this.nodeId = nodeId;
        this.channel = channel;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public Integer getChannel() { return channel; }
    public void setChannel(Integer channel) { this.channel = channel; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
