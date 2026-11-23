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
import com.proxy.app.Constants.Status;
import com.proxy.persistence.mongodb.model.DownlinkMessage;

@Service
public class DownlinkMessageRepositoryImpl implements DownlinkMessageRepositoryCustom {
	@Resource
	private MongoOperations mongoTemplate;

	public DownlinkMessage getDownlinkMessageByMessageId(String deviceId, long messageId) {
		Query query = new Query(Criteria.where("deviceId").is(deviceId).and("messageId").is(messageId));

		List<DownlinkMessage> vehicles = mongoTemplate.find(query, DownlinkMessage.class);
		return vehicles == null ? null : (vehicles.size() == 0 ? null : vehicles.get(0));
	}

	public long updateDownlinkMessage(DownlinkMessage downlinkSendStatus) {
		Query query = new Query(Criteria.where("deviceId").is(downlinkSendStatus.getDeviceId()));

		Update update = new Update();
		update.set("eventId", downlinkSendStatus.getEventId());
		update.set("ip", downlinkSendStatus.getIp());
		update.set("messageBytes", downlinkSendStatus.getMessageBytes());
		update.set("messageId", downlinkSendStatus.getMessageId());
		update.set("messageTime", downlinkSendStatus.getMessageTime());
		update.set("port", downlinkSendStatus.getPort());
		update.set("reSendCount", downlinkSendStatus.getResendCount());
		update.set("sendTime", downlinkSendStatus.getSendTime());
		update.set("lastSendTime", downlinkSendStatus.getLastSendTime());
		update.set("serviceType", downlinkSendStatus.getServiceType());
		update.set("status", downlinkSendStatus.getStatus());
		update.set("vehicleIdentifier", downlinkSendStatus.getDeviceId());
		update.set("messageTime", downlinkSendStatus.getMessageTime());

		UpdateResult result = mongoTemplate.updateMulti(query, update, DownlinkMessage.class);
		return result.getModifiedCount();
	}

	public int deleteDownlinkMessageByMessageId(String deviceId, long messageId) {
		Query query = new Query(Criteria.where("deviceId").is(deviceId).and("messageId").is(messageId));
		DeleteResult result = mongoTemplate.remove(query, DownlinkMessage.class);
		return (int)result.getDeletedCount();
	}

	public int deleteAllAckDownlinkMessage(Status status) {
		Query query = new Query(Criteria.where("status").is(status.ACKNOWLEDGED));
		DeleteResult result = mongoTemplate.remove(query, DownlinkMessage.class);
		return (int)result.getDeletedCount();
	}

	public List<DownlinkMessage> queryOverResendMessages(int count) {
		Query query = new Query(Criteria.where("resendCount").gt(count));
		List<DownlinkMessage> messages = mongoTemplate.find(query, DownlinkMessage.class);
		return messages;
	}

	public List<DownlinkMessage> queryAllNotAckRecords(Status status) {
		Query query = new Query(Criteria.where("status").ne(status.ACKNOWLEDGED));

		List<DownlinkMessage> messages = mongoTemplate.find(query, DownlinkMessage.class);
		return messages;
	}

	public int deleteOverResendMessages(int resendCount) {
		Query query = new Query(Criteria.where("resendCount").gt(resendCount));
		mongoTemplate.remove(query, DownlinkMessage.class);
		return 0;
	}

	public List<DownlinkMessage> queryAllDownlinkMessages() {
		return mongoTemplate.findAll(DownlinkMessage.class);
	}
	public List<DownlinkMessage> queryAllDownlinkMessagesExceptStatus(Status p_status){
		Query query = new Query(Criteria.where("status").ne(p_status));

		List<DownlinkMessage> messages = mongoTemplate.find(query, DownlinkMessage.class);
		return messages;
	}

	public List<DownlinkMessage> queryAllDownlinkMessagesExceptStatusSince(Status p_status, Date since){
		Criteria criteria = Criteria.where("status").ne(p_status);
		if (since != null) {
			criteria = criteria.and("messageTime").gte(since);
		}
		List<DownlinkMessage> messages = mongoTemplate.find(new Query(criteria), DownlinkMessage.class);
		return messages;
	}

	public int updateExpiredDownlinkMessagesFailed(Date expireBefore){
		Query query = new Query(Criteria.where("status").ne(Status.FAILED).and("messageTime").lt(expireBefore));
		Update update = new Update().set("status", Status.FAILED);
		UpdateResult result = mongoTemplate.updateMulti(query, update, DownlinkMessage.class);
		return (int) result.getModifiedCount();
	}
	
	public List<DownlinkMessage> queryVehicleDownlinkMessagesExceptStatus(String deviceId, Status p_status){
		Query query = new Query(Criteria.where("deviceId").is(deviceId).and("status").ne(p_status));

		List<DownlinkMessage> messages = mongoTemplate.find(query, DownlinkMessage.class);
		return messages;
	}
	
	public List<DownlinkMessage> queryAllDownlinkMessagesWithStatus(Status p_status){
		Query query = new Query(Criteria.where("status").is(p_status));

		List<DownlinkMessage> messages = mongoTemplate.find(query, DownlinkMessage.class);
		return messages;
	}
}
