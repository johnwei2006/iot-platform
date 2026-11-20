package com.proxy.persistence.h2.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "DeviceInfo")
public class DeviceInfo {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="id")
	private Integer id;	
	
	@Column(name = "deviceId")
	private String deviceId;
	
	@Column(name = "ip")
	private String ip;
	
	@Column(name = "port")
	private int port;
	
	@Column(name = "lastConnectedTime")
	private Date lastConnectedTime;
	
	@Column(name = "status")
	private int status;
	
	@Column(name = "transportProtocol")
	private short transportProtocol;

	@Column(name = "transportProtocolVersion")
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

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
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
		DeviceInfo device = new DeviceInfo();
		device.setIp(info.getIp());
		device.setLastConnectedTime(info.getLastConnectedTime());
		device.setPort(info.getPort());
		device.setStatus(info.getStatus());
		device.setTransportProtocol(info.getTransportProtocol());
		device.setTransportProtocolVersion(info.getTransportProtocolVersion());
		device.setDeviceId(info.getDeviceId());
		return device;
	}
}
