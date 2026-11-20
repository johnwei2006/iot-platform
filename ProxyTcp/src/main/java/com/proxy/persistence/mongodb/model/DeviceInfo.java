package com.proxy.persistence.mongodb.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

//@Document(collection="VehicleInfo")
public class DeviceInfo {
	@Id
	private ObjectId id;	

	private String deviceId;

	private String ip;

	private int port;

	private Date lastConnectedTime;

	private int status;

	private short transportProtocol;

	private short transportProtocolVersion;

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

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
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

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
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
	
	public static DeviceInfo fromDeviceInfo(com.proxy.persistence.model.DeviceInfo info) {
		DeviceInfo vehicle = new DeviceInfo();
		vehicle.setIp(info.getIp());
		vehicle.setLastConnectedTime(info.getLastConnectedTime());
		vehicle.setPort(info.getPort());
		vehicle.setStatus(info.getStatus());
		vehicle.setTransportProtocol(info.getTransportProtocol());
		vehicle.setTransportProtocolVersion(info.getTransportProtocolVersion());
		vehicle.setDeviceId(info.getDeviceId());
		return vehicle;
	}
}
