package com.proxy.persistence;

import java.util.Date;
import java.util.List;

import com.proxy.persistence.model.DeviceInfo;

/**
 * 设备信息存储接口。Mongo 与 H2 两种实现各自 {@code @Profile} 注入唯一 Bean。
 */
public interface DeviceInfoService {
	DeviceInfo queryDeviceInfo(String identifier);

	DeviceInfo getDeviceInfoByIp(String ip, int port);

	int updateDeviceStatus(DeviceInfo vehicleStatus);

	List<DeviceInfo> getAllDeviceInfo();

	List<DeviceInfo> queryDeviceInfoNotLaterThan(Date activeTime);

	int deleteDeviceInfo(String deviceId);
}
