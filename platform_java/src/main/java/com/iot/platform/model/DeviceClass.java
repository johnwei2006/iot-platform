package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 控制设备大类字典。 */
@TableName("iot_device_class")
public class DeviceClass {

    @TableId(value = "class", type = IdType.INPUT)
    private String deviceClass;
    private String className;
    /** 开关模式：1-开度；2-2开设备；3-三开设备 */
    private Integer switchMode;
    private String onName;
    private String offName;
    private String stopName;
    private String setName;

    public String getDeviceClass() { return deviceClass; }
    public void setDeviceClass(String deviceClass) { this.deviceClass = deviceClass; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public Integer getSwitchMode() { return switchMode; }
    public void setSwitchMode(Integer switchMode) { this.switchMode = switchMode; }
    public String getOnName() { return onName; }
    public void setOnName(String onName) { this.onName = onName; }
    public String getOffName() { return offName; }
    public void setOffName(String offName) { this.offName = offName; }
    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }
    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }
}
