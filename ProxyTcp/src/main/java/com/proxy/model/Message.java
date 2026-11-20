package com.proxy.model;

import java.util.Date;

public class Message {
	public static int CONNECTTYPE_UDP = 1;
	public static int CONNECTTYPE_TCP = 2;
	private String ip;
	private int port;
	private byte[] payload;
	private int connectType;
	private Date messageTime;
	private String deviceId;

	public byte[] getPayload() {
		return payload;
	}

	public int getConnectType() {
		return connectType;
	}

	public void setConnectType(int connectType) {
		this.connectType = connectType;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public Date getMessageTime() {
		return messageTime;
	}

	public void setMessageTime(Date messageTime) {
		this.messageTime = messageTime;
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

	public synchronized String getDeviceId() {
		return deviceId;
	}

	public synchronized void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
}
