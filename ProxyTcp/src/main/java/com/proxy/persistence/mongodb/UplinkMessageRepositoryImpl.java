package com.proxy.persistence.mongodb;

import java.util.Date;
import java.util.List;

import jakarta.annotation.Resource;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import com.mongodb.client.result.DeleteResult;
import com.proxy.persistence.mongodb.model.UplinkMessage;

@Service
@Profile("mongo")
public class UplinkMessageRepositoryImpl implements UplinkMessageRepositoryCustom{
    @Resource
    private MongoOperations mongoTemplate;

    public UplinkMessage queryUplinkMessage(String deviceId, long messageId) {
        Query query = new Query(Criteria.where("deviceId").is(deviceId).and("messageId").is(messageId));
        
        List<UplinkMessage> vehicles = mongoTemplate.find(query, UplinkMessage.class);
        return vehicles == null ? null : (vehicles.size() == 0 ? null : vehicles.get(0));
	}

    public int saveUplinkMessage(com.proxy.persistence.model.UplinkMessage uplink) {
    	UplinkMessage message = UplinkMessage.fromUplinkMessage(uplink);
    	message = mongoTemplate.save(message);
    	if (message == null)
    		return 0;
    	return 1;
    }
	public int deleteUplinkMessage(String deviceId, long messageId) {
        Query query = new Query(Criteria.where("deviceId").is(deviceId).and("messageId").is(messageId));
        DeleteResult result = mongoTemplate.remove(query, UplinkMessage.class);
        return (int)result.getDeletedCount();
	}

	public int deleteUplinkMessageByVehicle(String deviceId) {
        Query query = new Query(Criteria.where("deviceId").is(deviceId));
        DeleteResult result = mongoTemplate.remove(query, UplinkMessage.class);
        return (int)result.getDeletedCount();
	}
	
	public int deleteUplinkMessageLaterThanTime(Date receivedTime) {
        Query query = new Query(Criteria.where("receivedTime").lt(receivedTime));
        DeleteResult result = mongoTemplate.remove(query, UplinkMessage.class);
        return (int)result.getDeletedCount();
	}

	public List<UplinkMessage> queryAllUplinkMessages(){
        return mongoTemplate.findAll(UplinkMessage.class);
	}
}
