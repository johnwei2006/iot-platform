package com.proxy.persistence.mongodb.model;

import java.util.Date;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import com.proxy.app.Constants.Status;

public class DownlinkMessage {
	@Id
	private ObjectId id;	

	private String deviceId;

	private long eventId;

	private int serviceType;

	private long messageId;

	private byte[] messageBytes;

	private String ip;

	private int port;

	private Date messageTime;

	private Date sendTime;

	private Date lastSendTime;

	private int resendCount;

	private Status status;

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long m_messageId) {
		this.messageId = m_messageId;
	}

	public Date getSendTime() {
		return sendTime;
	}

	public void setSendTime(Date sendTime) {
		this.sendTime = sendTime;
	}

	public Date getLastSendTime() {
		return lastSendTime;
	}

	public void setLastSendTime(Date lastSendTime) {
		this.lastSendTime = lastSendTime;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}


	public int getResendCount() {
		return resendCount;
	}

	public void setResendCount(int resendCount) {
		this.resendCount = resendCount;
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

	public Date getMessageTime() {
		return messageTime;
	}

	public void setMessageTime(Date messageTime) {
		this.messageTime = messageTime;
	}

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public void setMessageBytes(byte[] messageBytes) {
		this.messageBytes = messageBytes;
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
	
	public com.proxy.persistence.model.DownlinkMessage toDownlinkMessage(){
		com.proxy.persistence.model.DownlinkMessage msg = new com.proxy.persistence.model.DownlinkMessage();
		msg.setEventId(this.eventId);
		msg.setIp(ip);
		msg.setMessageBytes(messageBytes);
		msg.setMessageId(messageId);
		msg.setMessageTime(messageTime);
		msg.setPort(port);
		msg.setResendCount(resendCount);
		msg.setSendTime(sendTime);
		msg.setLastSendTime(lastSendTime);
		msg.setServiceType(serviceType);
		msg.setDeviceId(deviceId);
		msg.setStatus(status);
		return msg;
	}
	
	public DownlinkMessage fromDownlinkMessage(com.proxy.persistence.model.DownlinkMessage downlinkSendStatus) {
		this.setDeviceId(downlinkSendStatus.getDeviceId());
		this.setEventId(downlinkSendStatus.getEventId());
		this.setServiceType(downlinkSendStatus.getServiceType());
		this.setMessageBytes(downlinkSendStatus.getMessageBytes());
		this.setMessageId(downlinkSendStatus.getMessageId());
		this.setStatus(downlinkSendStatus.getStatus());
		this.setResendCount(downlinkSendStatus.getResendCount());
		this.setIp(downlinkSendStatus.getIp());
		this.setPort(downlinkSendStatus.getPort());
		this.setMessageTime(downlinkSendStatus.getMessageTime());
		this.setSendTime(downlinkSendStatus.getSendTime());
		this.setLastSendTime(downlinkSendStatus.getLastSendTime());
		return this;
	}
}
