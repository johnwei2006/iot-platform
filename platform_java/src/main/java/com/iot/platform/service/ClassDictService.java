package com.iot.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.mapper.DeviceClassMapper;
import com.iot.platform.mapper.DeviceClassTypeMapper;
import com.iot.platform.mapper.SensorClassMapper;
import com.iot.platform.mapper.SensorClassTypeMapper;
import com.iot.platform.model.DeviceClass;
import com.iot.platform.model.DeviceClassType;
import com.iot.platform.model.SensorClass;
import com.iot.platform.model.SensorClassType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** 传感器/控制设备分类字典聚合服务（4 张字典表只读查询）。 */
@Service
public class ClassDictService {

    @Autowired
    private SensorClassMapper sensorClassMapper;
    @Autowired
    private SensorClassTypeMapper sensorClassTypeMapper;
    @Autowired
    private DeviceClassMapper deviceClassMapper;
    @Autowired
    private DeviceClassTypeMapper deviceClassTypeMapper;

    public List<SensorClass> listSensorClass() {
        return sensorClassMapper.selectList(Wrappers.emptyWrapper());
    }

    public List<SensorClassType> listSensorClassType(String sensorClass) {
        return sensorClassTypeMapper.selectList(sensorClass == null || sensorClass.isEmpty()
                ? Wrappers.emptyWrapper()
                : Wrappers.<SensorClassType>lambdaQuery().eq(SensorClassType::getSensorClass, sensorClass));
    }

    public List<DeviceClass> listDeviceClass() {
        return deviceClassMapper.selectList(Wrappers.emptyWrapper());
    }

    public List<DeviceClassType> listDeviceClassType(String deviceClass) {
        return deviceClassTypeMapper.selectList(deviceClass == null || deviceClass.isEmpty()
                ? Wrappers.emptyWrapper()
                : Wrappers.<DeviceClassType>lambdaQuery().eq(DeviceClassType::getDeviceClass, deviceClass));
    }
}
