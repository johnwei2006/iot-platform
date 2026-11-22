package com.proxy.persistence.h2.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "UplinkMessage")
public class UplinkMessage {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="id")
	private Integer id;	
	
	@Column(name = "deviceId")
	private String deviceId;
	
	@Column(name = "messageId")
	private long messageId;

	@Column(name = "receivedTime")
	private Date receivedTime;

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
