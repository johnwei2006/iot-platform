package com.proxy.persistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proxy.persistence.h2.DeviceInfoRespository;
import com.proxy.persistence.h2.model.DeviceInfo;

@org.springframework.context.annotation.Profile("h2")
@Service
public class DeviceInfoServiceH2 implements DeviceInfoService {
	@Autowired
	DeviceInfoRespository deviceStatusRespository;
	
	private Lock lock = new ReentrantLock();
	
	public com.proxy.persistence.model.DeviceInfo queryDeviceInfo(String identifier){
		DeviceInfo device = deviceStatusRespository.queryDeviceInfoByDevice(identifier);
		return device == null ? null : device.toDeviceInfo();
	}
	
	public com.proxy.persistence.model.DeviceInfo getDeviceInfoByIp(String ip, int port) {
		DeviceInfo vehicle = deviceStatusRespository.getDeviceInfoByIp(ip, port);
		return vehicle == null ? null : vehicle.toDeviceInfo();
	}
	
	@Transactional
	public int updateDeviceStatus(com.proxy.persistence.model.DeviceInfo vehicleStatus) {
		try {
			lock.lock();
			
			DeviceInfo status  = deviceStatusRespository.queryDeviceInfoByDevice(vehicleStatus.getDeviceId());
			if (status == null) {
				status = DeviceInfo.fromDeviceInfo(vehicleStatus);
				deviceStatusRespository.save(status);
				return 1;
			} else {
				return deviceStatusRespository.updateDeviceInfo(vehicleStatus.getDeviceId(), vehicleStatus.getIp(),vehicleStatus.getPort(), vehicleStatus.getTransportProtocol(), vehicleStatus.getTransportProtocolVersion(), vehicleStatus.getLastConnectedTime());
			}
		} finally {
			lock.unlock();
		}
	}
	
	public List<com.proxy.persistence.model.DeviceInfo> getAllDeviceInfo(){
		Iterator<DeviceInfo> s = deviceStatusRespository.findAll().iterator();
		List<com.proxy.persistence.model.DeviceInfo> status = new ArrayList<com.proxy.persistence.model.DeviceInfo>();
		while (s.hasNext()) {
			status.add(s.next().toDeviceInfo());
		}
		return status;
	}
	
	public List<com.proxy.persistence.model.DeviceInfo> queryDeviceInfoNotLaterThan(Date activeTime) {
		List<com.proxy.persistence.model.DeviceInfo> result = new ArrayList<com.proxy.persistence.model.DeviceInfo>();
		List<DeviceInfo> vehicles = deviceStatusRespository.queryDeviceInfoNotLaterThan(activeTime);
		if (vehicles == null)
			return null;
		else {
			for (int i = 0; i < vehicles.size(); i++) {
				result.add(vehicles.get(i).toDeviceInfo());
			}
			return result;
		}
	}
	
	public int deleteDeviceInfo(String vehicleIdentifier) {
		return deviceStatusRespository.deleteDeviceInfo(vehicleIdentifier);
	}
}
