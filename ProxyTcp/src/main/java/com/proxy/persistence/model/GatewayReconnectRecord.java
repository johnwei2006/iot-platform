package com.proxy.persistence.model;

import java.util.Date;

/** 网关连接/重连记录（服务层模型） */
public class GatewayReconnectRecord {

	private String deviceId;
	private String ip;
	private int port;
	private Date connectTime;
	/** FIRST / RECONNECT */
	private String type;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
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

	public Date getConnectTime() {
		return connectTime;
	}

	public void setConnectTime(Date connectTime) {
		this.connectTime = connectTime;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
