package com.proxy.persistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proxy.persistence.mongodb.DeviceInfoRepository;
import com.proxy.persistence.mongodb.model.DeviceInfo;

@org.springframework.context.annotation.Profile("mongo")
@Service
public class DeviceInfoServiceMongo implements DeviceInfoService {
	@Autowired
	DeviceInfoRepository deviceStatusRespository;
	
	private Lock lock = new ReentrantLock();
	
	public com.proxy.persistence.model.DeviceInfo queryDeviceInfo(String identifier){
		DeviceInfo info = deviceStatusRespository.queryDeviceInfo(identifier);
		return info == null ? null : info.toDeviceInfo();
	}
	
	public com.proxy.persistence.model.DeviceInfo getDeviceInfoByIp(String ip, int port) {
		DeviceInfo info = deviceStatusRespository.getDeviceInfoByIp(ip, port);
		return info == null ? null : info.toDeviceInfo();
	}
	
	@Transactional
	public int updateDeviceStatus(com.proxy.persistence.model.DeviceInfo vehicleStatus) {
		try {
			lock.lock();
			
			DeviceInfo status  = deviceStatusRespository.queryDeviceInfo(vehicleStatus.getDeviceId());
			if (status == null) {
				status = DeviceInfo.fromDeviceInfo(vehicleStatus);
				deviceStatusRespository.save(status);
				return 1;
			} else {
				status.setIp(vehicleStatus.getIp());
				status.setPort(vehicleStatus.getPort());
				status.setLastConnectedTime(vehicleStatus.getLastConnectedTime());
				status.setStatus(vehicleStatus.getStatus());
				status.setTransportProtocol(vehicleStatus.getTransportProtocol());
				status.setTransportProtocolVersion(vehicleStatus.getTransportProtocolVersion());
				status.setDeviceId(vehicleStatus.getDeviceId());
				return deviceStatusRespository.updateDeviceStatus(status);
			}
		} finally {
			lock.unlock();
		}
	}
	
	public List<com.proxy.persistence.model.DeviceInfo> getAllDeviceInfo(){
		Sort sort = Sort.by(Sort.Direction.ASC, "lastConnectedTime");
		Iterator<DeviceInfo> s = deviceStatusRespository.findAll(sort).iterator();
		List<com.proxy.persistence.model.DeviceInfo> status = new ArrayList<com.proxy.persistence.model.DeviceInfo>();
		while (s.hasNext()) {
			status.add(s.next().toDeviceInfo());
		}
		return status;
	}
	
	public List<com.proxy.persistence.model.DeviceInfo> queryDeviceInfoNotLaterThan(Date activeTime) {
		List<com.proxy.persistence.model.DeviceInfo> result = new ArrayList<com.proxy.persistence.model.DeviceInfo>();
		List<DeviceInfo> devices = deviceStatusRespository.queryDeviceInfoNotLaterThan(activeTime);
		if (devices == null)
			return null;
		else {
			for (int i = 0; i < devices.size(); i++) {
				result.add(devices.get(i).toDeviceInfo());
			}
			return result;
		}
	}
	
	public int deleteDeviceInfo(String deviceId) {
		return deviceStatusRespository.deleteDeviceInfoById(deviceId);
	}
}
