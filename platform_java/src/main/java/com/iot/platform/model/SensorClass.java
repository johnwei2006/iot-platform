package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 传感器大类字典。 */
@TableName("iot_sensor_class")
public class SensorClass {

    @TableId(value = "class", type = IdType.INPUT)
    private String sensorClass;
    private String className;
    private String classNameEn;
    private String unit;
    private Integer scale;

    public String getSensorClass() { return sensorClass; }
    public void setSensorClass(String sensorClass) { this.sensorClass = sensorClass; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getClassNameEn() { return classNameEn; }
    public void setClassNameEn(String classNameEn) { this.classNameEn = classNameEn; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Integer getScale() { return scale; }
    public void setScale(Integer scale) { this.scale = scale; }
}
