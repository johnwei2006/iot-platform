package com.proxy.persistence.mongodb;

import java.util.List;

import com.proxy.persistence.mongodb.model.SendFailureMessage;

public interface SendFailureMessageRepositoryCustom{
	public SendFailureMessage getSendFailureMessageByMessageId(String vehicleIdentifier, long messageId);
	public int  deleteSendFailureMessageByMessageId(String vehicleIdentifier, long messageId);
	public List<SendFailureMessage> queryAllSendFailureMessages();
}
