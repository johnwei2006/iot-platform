package com.proxy.persistence.h2.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.proxy.Constants.Status;

@Entity
@Table(name = "DownlinkMessage")
public class DownlinkMessage {
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

	@Column(name = "messageId")
	private long messageId;
	
	@Column(name = "messageBytes", columnDefinition = "BINARY(1048576)")
	private byte[] messageBytes;

	@Column(name = "ip")
	private String ip;

	@Column(name = "port")
	private int port;
	
	@Column(name = "messageTime")
	private Date messageTime;
	
	@Column(name = "sendTime")
	private Date sendTime;
	
	@Column(name = "lastSendTime")
	private Date lastSendTime;
	
	@Column(name = "resendCount")
	private int resendCount;
	
	@Column(name = "status")
	private Status status;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
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
	
	public static DownlinkMessage fromDownlinkMessage(com.proxy.persistence.model.DownlinkMessage msg) {
		DownlinkMessage downlink = new DownlinkMessage();
		downlink.setDeviceId(msg.getDeviceId());
		downlink.setEventId(msg.getEventId());
		downlink.setServiceType(msg.getServiceType());
		downlink.setMessageBytes(msg.getMessageBytes());
		downlink.setMessageId(msg.getMessageId());
		downlink.setStatus(msg.getStatus());
		downlink.setResendCount(msg.getResendCount());
		downlink.setIp(msg.getIp());
		downlink.setPort(msg.getPort());
		downlink.setMessageTime(msg.getMessageTime());
		downlink.setSendTime(msg.getSendTime());
		downlink.setLastSendTime(msg.getLastSendTime());
		return downlink;
	}
}
