package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 传感器小类（类型）字典。 */
@TableName("iot_sensor_class_type")
public class SensorClassType {

    @TableId(value = "class_type", type = IdType.INPUT)
    private String classType;
    /** 传感器大类编码（列名 class，Java 字段避免与保留概念混淆） */
    @TableField("class")
    private String sensorClass;
    private String classTypeName;
    private String model;
    private String description;

    public String getClassType() { return classType; }
    public void setClassType(String classType) { this.classType = classType; }
    public String getSensorClass() { return sensorClass; }
    public void setSensorClass(String sensorClass) { this.sensorClass = sensorClass; }
    public String getClassTypeName() { return classTypeName; }
    public void setClassTypeName(String classTypeName) { this.classTypeName = classTypeName; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
