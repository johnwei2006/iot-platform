package com.proxy.persistence.mongodb;

import java.util.Date;
import java.util.List;
import jakarta.annotation.Resource;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.proxy.persistence.mongodb.model.DeviceInfo;

@Service
public class DeviceInfoRepositoryImpl implements DeviceInfoRepositoryCustom{
	
    @Resource
    private MongoOperations mongoTemplate;

    public DeviceInfo queryDeviceInfo(String identifier) {
        Query query = new Query(Criteria.where("deviceId").is(identifier));
        List<DeviceInfo> devices = mongoTemplate.find(query, DeviceInfo.class);
        return (devices == null || devices.size() == 0) ? null : devices.get(0);
	}
	public DeviceInfo getDeviceInfoByIp(String ip, int port) {
        Query query = new Query(Criteria.where("ip").is(ip).and("port").is(port));
        
        List<DeviceInfo> vehicles = mongoTemplate.find(query, DeviceInfo.class);
        return vehicles != null ? vehicles.get(0) : null;
	}

	public List<DeviceInfo> queryDeviceInfoNotLaterThan(Date activeTime) {
        Query query = new Query(Criteria.where("lastConnectedTime").gt(activeTime));
        return mongoTemplate.find(query, DeviceInfo.class);
	}

	public int updateDeviceStatus(DeviceInfo status) {
		Query query = new Query(Criteria.where("deviceId").is(status.getDeviceId()));

		Update update = new Update();
		update.set("ip", status.getIp());
		update.set("lastConnectedTime", status.getLastConnectedTime());
		update.set("port", status.getPort());
		update.set("status", status.getStatus());
		update.set("transportProtocol", status.getTransportProtocol());
		update.set("transportProtocolVersion", status.getTransportProtocolVersion());

		UpdateResult result = mongoTemplate.updateMulti(query, update, DeviceInfo.class);
		return (int)result.getModifiedCount();
	}
	public List<DeviceInfo> getAllDeviceInfo(){
        return mongoTemplate.findAll(DeviceInfo.class);
	}
	
	public int deleteDeviceInfoById(String deviceId) {
		Query query = new Query(Criteria.where("deviceId").is(deviceId));
		DeleteResult result = mongoTemplate.remove(query, DeviceInfo.class);
		return (int)result.getDeletedCount();
	}
}
