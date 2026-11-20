package com.proxy.persistence.mongodb;

import java.util.Date;
import java.util.List;

import com.proxy.persistence.mongodb.model.DeviceInfo;

public interface DeviceInfoRepositoryCustom{
    //void ping();
	public DeviceInfo queryDeviceInfo(String identifier);
	public DeviceInfo getDeviceInfoByIp(String ip, int port);
	public int updateDeviceStatus(DeviceInfo vehicleStatus);
	public List<DeviceInfo> getAllDeviceInfo();
	public List<DeviceInfo> queryDeviceInfoNotLaterThan(Date activeTime);
	public int deleteDeviceInfoById(String deviceId);
}
