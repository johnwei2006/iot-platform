package com.proxy.persistence.mongodb;

import java.util.List;
import jakarta.annotation.Resource;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import com.mongodb.client.result.DeleteResult;
import com.proxy.persistence.mongodb.model.SendFailureMessage;

@Service
@Profile("mongo")
public class SendFailureMessageRepositoryImpl implements SendFailureMessageRepositoryCustom {
	@Resource
	private MongoOperations mongoTemplate;

	public SendFailureMessage getSendFailureMessageByMessageId(String deviceId, long messageId) {
		Query query = new Query(Criteria.where("deviceId").is(deviceId).and("messageId").is(messageId));

		List<SendFailureMessage> vehicles = mongoTemplate.find(query, SendFailureMessage.class);
		return vehicles == null ? null : (vehicles.size() == 0 ? null : vehicles.get(0));
	}

	public int deleteSendFailureMessageByMessageId(String deviceId, long messageId) {
		Query query = new Query(Criteria.where("deviceId").is(deviceId).and("messageId").is(messageId));
		DeleteResult result = mongoTemplate.remove(query, SendFailureMessage.class);
		return (int)result.getDeletedCount();
	}

	public List<SendFailureMessage> queryAllSendFailureMessages() {
		return mongoTemplate.findAll(SendFailureMessage.class);
	}
}
