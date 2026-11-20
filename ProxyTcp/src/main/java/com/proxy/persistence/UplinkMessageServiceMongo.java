package com.proxy.persistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proxy.persistence.mongodb.UplinkMessageRepository;
import com.proxy.persistence.mongodb.model.UplinkMessage;

@org.springframework.context.annotation.Profile("mongo")
@Service
public class UplinkMessageServiceMongo implements UplinkMessageService {
	@Autowired
	UplinkMessageRepository uplinkMessageStatusRespository;
	
	public com.proxy.persistence.model.UplinkMessage getUplinkMessageById(String identifier, long messageId){
		UplinkMessage msg =  uplinkMessageStatusRespository.queryUplinkMessage(identifier, messageId);
		return msg == null ? null : msg.toUplinkMessage();
	}
	
	@Transactional
	public int saveUplinkMessage(com.proxy.persistence.model.UplinkMessage uplink){
		return  uplinkMessageStatusRespository.saveUplinkMessage(uplink);
	}

	@Transactional
	public int deleteUplinkMessageStatus(String identifier, long messageId){
		UplinkMessage status  = uplinkMessageStatusRespository.queryUplinkMessage(identifier, messageId);
		if (status != null) {
			return uplinkMessageStatusRespository.deleteUplinkMessage(identifier, messageId);
		} else {
			return 0;
		}
	}

	@Transactional
	public int deleteUplinkMessageStatusByVehicle(String identifier){
		return uplinkMessageStatusRespository.deleteUplinkMessageByVehicle(identifier);
	}

	@Transactional
	public int deleteUplinkMessageLaterThanTime(Date receivedTime) {
		return uplinkMessageStatusRespository.deleteUplinkMessageLaterThanTime(receivedTime);
	}

	public List<com.proxy.persistence.model.UplinkMessage> queryAllUplinkMessages(){
		List<UplinkMessage> msgs = uplinkMessageStatusRespository.queryAllUplinkMessages();
		if (msgs == null || msgs.size() == 0)
			return null;
		List<com.proxy.persistence.model.UplinkMessage> results = new ArrayList<com.proxy.persistence.model.UplinkMessage>();
		for (int i = 0; i < msgs.size(); i++) {
			results.add(msgs.get(i).toUplinkMessage());
		}
		return results;
	}
}
