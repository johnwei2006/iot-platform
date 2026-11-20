package com.proxy.persistence.h2.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "SendFailureMessage")
public class SendFailureMessage {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="id")
	private Integer id;	
	
	@Column(name = "deviceId")
	private String deviceId;
	
	@Column(name = "eventId")
	private long eventId;

	@Column(name = "serviceType")
	private int serviceType;

	@Column(name = "serviceVersion")
	private int serviceVersion;

	@Column(name = "messageId")
	private long messageId;
	
	@Column(name = "ip")
	private String ip;

	@Column(name = "port")
	private int port;
	
	@Column(name = "sourceServiceType")
	private int sourceServiceType;
	
	@Column(name = "errorCode")
	private int errorCode;;

	@Column(name = "errorMessage")
	private String errorMessage;

	public synchronized String getDeviceId() {
		return deviceId;
	}

	public synchronized void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long m_messageId) {
		this.messageId = m_messageId;
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
	
	public int getServiceVersion() {
		return serviceVersion;
	}

	public void setServiceVersion(int serviceVersion) {
		this.serviceVersion = serviceVersion;
	}

	public int getSourceServiceType() {
		return sourceServiceType;
	}

	public void setSourceServiceType(int sourceServiceType) {
		this.sourceServiceType = sourceServiceType;
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

	public com.proxy.persistence.model.SendFailureMessage toSendFailureMessage(){
		com.proxy.persistence.model.SendFailureMessage msg = new com.proxy.persistence.model.SendFailureMessage();
		msg.setEventId(this.eventId);
		msg.setIp(ip);
		msg.setMessageId(messageId);
		msg.setPort(port);
		msg.setServiceType(serviceType);
		msg.setDeviceId(deviceId);
		msg.setServiceVersion(serviceVersion);
		msg.setSourceServiceType(sourceServiceType);
		msg.setErrorCode(errorCode);
		msg.setErrorMessage(errorMessage);
		return msg;
	}
	
	public static SendFailureMessage fromSendFailureMessage(com.proxy.persistence.model.SendFailureMessage msg) {
		SendFailureMessage message = new SendFailureMessage();
		message.setDeviceId(msg.getDeviceId());
		message.setEventId(msg.getEventId());
		message.setServiceType(msg.getServiceType());
		message.setServiceVersion(msg.getServiceVersion());
		message.setMessageId(msg.getMessageId());
		message.setIp(msg.getIp());
		message.setPort(msg.getPort());
		message.setSourceServiceType(msg.getSourceServiceType());
		message.setErrorCode(msg.getErrorCode());
		message.setErrorMessage(msg.getErrorMessage());
		return message;
	}
}
