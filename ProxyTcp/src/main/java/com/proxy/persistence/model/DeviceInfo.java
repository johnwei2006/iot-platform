package com.proxy.persistence.model;

import java.util.Date;

public class DeviceInfo {

	private String deviceId;

	private String ip;

	private int port;

	private Date lastConnectedTime;

	private int status;

	private short transportProtocol;

	private short transportProtocolVersion;

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

	public Date getLastConnectedTime() {
		return lastConnectedTime;
	}

	public void setLastConnectedTime(Date lastConnectedTime) {
		this.lastConnectedTime = lastConnectedTime;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public short getTransportProtocol() {
		return transportProtocol;
	}

	public void setTransportProtocol(short transportProtocol) {
		this.transportProtocol = transportProtocol;
	}

	public short getTransportProtocolVersion() {
		return transportProtocolVersion;
	}

	public void setTransportProtocolVersion(short transportProtocolVersion) {
		this.transportProtocolVersion = transportProtocolVersion;
	}
	
	public com.proxy.persistence.model.DeviceInfo toDeviceInfo(){
		com.proxy.persistence.model.DeviceInfo info = new com.proxy.persistence.model.DeviceInfo();
		info.setIp(ip);
		info.setLastConnectedTime(lastConnectedTime);
		info.setPort(port);
		info.setStatus(status);
		info.setTransportProtocol(transportProtocol);
		info.setTransportProtocolVersion(transportProtocolVersion);
		info.setDeviceId(deviceId);
		return info;
	}
	
	public DeviceInfo fromVehicleInfo(com.proxy.persistence.model.DeviceInfo info) {
		this.setIp(info.getIp());
		this.setLastConnectedTime(info.getLastConnectedTime());
		this.setPort(info.getPort());
		this.setStatus(info.getStatus());
		this.setTransportProtocol(info.getTransportProtocol());
		this.setTransportProtocolVersion(info.getTransportProtocolVersion());
		this.setDeviceId(info.getDeviceId());
		return this;
	}
}
