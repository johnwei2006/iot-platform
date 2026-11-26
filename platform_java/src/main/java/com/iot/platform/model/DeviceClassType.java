package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/** 控制设备小类（类型）字典。 */
@TableName("iot_device_class_type")
public class DeviceClassType {

    /** 类型编码（四位：device_class + XX） */
    @TableId(value = "type", type = IdType.INPUT)
    private String type;
    private String typeName;
    /** 设备大类编码（列名 device_class） */
    @TableField("device_class")
    private String deviceClass;
    private String model;
    private String descInfo;
    private Date updatedTime;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getDeviceClass() { return deviceClass; }
    public void setDeviceClass(String deviceClass) { this.deviceClass = deviceClass; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getDescInfo() { return descInfo; }
    public void setDescInfo(String descInfo) { this.descInfo = descInfo; }
    public Date getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Date updatedTime) { this.updatedTime = updatedTime; }
}
