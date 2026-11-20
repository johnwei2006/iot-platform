package com.proxy.persistence.mongodb.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

public class UplinkMessage {
	@Id
	private ObjectId id;	

	private String deviceId;

	private long messageId;

	private Date receivedTime;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long m_messageId) {
		this.messageId = m_messageId;
	}

	public Date getReceivedTime() {
		return receivedTime;
	}

	public void setReceivedTime(Date receivedTime) {
		this.receivedTime = receivedTime;
	}
	
	public com.proxy.persistence.model.UplinkMessage toUplinkMessage() {
		com.proxy.persistence.model.UplinkMessage result = new com.proxy.persistence.model.UplinkMessage();
		result.setMessageId(messageId);
		result.setReceivedTime(receivedTime);
		result.setDeviceId(deviceId);
		return result;
	}
	
	public static UplinkMessage fromUplinkMessage(com.proxy.persistence.model.UplinkMessage uplink) {
		UplinkMessage result = new UplinkMessage();
		result.setMessageId(uplink.getMessageId());
		result.setReceivedTime(uplink.getReceivedTime());
		result.setDeviceId(uplink.getDeviceId());
		return result;
	}
}
