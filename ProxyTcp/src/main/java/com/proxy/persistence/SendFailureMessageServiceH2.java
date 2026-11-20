package com.proxy.persistence;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proxy.persistence.h2.SendFailureMessageRespository;
import com.proxy.persistence.h2.model.SendFailureMessage;

@org.springframework.context.annotation.Profile("h2")
@Service
public class SendFailureMessageServiceH2 implements SendFailureMessageService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	SendFailureMessageRespository sendFailureMessageRespository;
	
	public List<com.proxy.persistence.model.SendFailureMessage> findAllSendFailureMessages(){
		List<SendFailureMessage> sendFailureMessages = sendFailureMessageRespository.queryAllSendFailureMessages();
		if (sendFailureMessages != null) {
			List<com.proxy.persistence.model.SendFailureMessage> results = new ArrayList<com.proxy.persistence.model.SendFailureMessage>();
			for(int i = 0; i < sendFailureMessages.size(); i++) {
				results.add(sendFailureMessages.get(i).toSendFailureMessage());
			}
			return results;
		} else {
			return null;
		}
	}
	
	public com.proxy.persistence.model.SendFailureMessage getSendFailureMessageByMessageId(String vehicleIdentifier, long messageId){
		SendFailureMessage msg = sendFailureMessageRespository.querySendFailureMessageByMessageId(vehicleIdentifier, messageId);
		return msg == null ? null : msg.toSendFailureMessage();
	}
	
	@Transactional
	public int  deleteSendFailureMessageByMessageId(String vehicleIdentifier, long messageId) {
		return sendFailureMessageRespository.deleteSendFailureMessageByMessageId(vehicleIdentifier, messageId);
	}
	
	@Transactional
	public boolean  saveSendFailureMessage(com.proxy.persistence.model.SendFailureMessage msg) {
		if (msg == null)
			return false;
		com.proxy.persistence.h2.model.SendFailureMessage result = sendFailureMessageRespository.save(com.proxy.persistence.h2.model.SendFailureMessage.fromSendFailureMessage(msg));
		if (result == null)
			return false;
		else 
			return true;
	}

}
