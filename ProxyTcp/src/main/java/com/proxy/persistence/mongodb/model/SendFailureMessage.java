package com.proxy.persistence.mongodb.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "sendFailureMessage")
public class SendFailureMessage {
	@Id
	private ObjectId id;	

	private String deviceId ;

	private long eventId ;

	private int serviceType ;

	private long messageId ;

	private String ip ;

	private int port ;

	private int serviceVersion ;

	@Field(value = "errorCode")
	private int errorCode ;
	
	private String errorMessage ;

	@Field(value = "sourceServiceType")
	private int sourceServiceType ;

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

	public long getEventId() {
		return eventId;
	}

	public void setEventId(long eventId) {
		this.eventId = eventId;
	}

	public int getServiceType() {
		return serviceType;
	}

	public void setServiceType(int serviceType) {
		this.serviceType = serviceType;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getServiceVersion() {
		return serviceVersion;
	}

	public void setServiceVersion(int serviceVersion) {
		this.serviceVersion = serviceVersion;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public int getSourceServiceType() {
		return sourceServiceType;
	}

	public void setSourceServiceType(int sourceServiceType) {
		this.sourceServiceType = sourceServiceType;
	}

	public com.proxy.persistence.model.SendFailureMessage toSendFailureMessage(){
		com.proxy.persistence.model.SendFailureMessage msg = new com.proxy.persistence.model.SendFailureMessage();
		msg.setEventId(this.eventId);
		msg.setIp(ip);
		msg.setMessageId(messageId);
		msg.setPort(port);
		msg.setServiceType(serviceType);
		msg.setDeviceId(deviceId);
		msg.setErrorCode(errorCode);
		msg.setErrorMessage(errorMessage);
		msg.setServiceVersion(serviceVersion);
		msg.setSourceServiceType(sourceServiceType);
		return msg;
	}
	
	public SendFailureMessage fromSendFailureMessage(com.proxy.persistence.model.SendFailureMessage msg) {
		this.setDeviceId(msg.getDeviceId());
		this.setEventId(msg.getEventId());
		this.setServiceType(msg.getServiceType());
		this.setMessageId(msg.getMessageId());
		this.setIp(msg.getIp());
		this.setPort(msg.getPort());
		this.setErrorCode(msg.getErrorCode());
		this.setErrorMessage(msg.getErrorMessage());
		this.setSourceServiceType(msg.getSourceServiceType());
		this.setServiceVersion(msg.getServiceVersion());
		return this;
	}
}
