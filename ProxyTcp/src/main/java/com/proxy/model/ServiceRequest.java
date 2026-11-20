package com.proxy.model;

import java.util.Date;

public class ServiceRequest {
	private String 	deviceId;
	private String 	nodeId;
	private int 	eventId;
	private Date 	eventCreationTime;
	private int 	serviceType;
	private short 	serviceVersion;
	private int 	serviceDataEncoding = 0;
	private int 	serviceDataProtocolVersion;
	private int 	serviceEncryption;
	private byte[] 	serviceData;

	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public synchronized String getNodeId() {
		return nodeId;
	}
	public synchronized void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	public int getServiceType() {
		return serviceType;
	}
	public void setServiceType(int serviceType) {
		this.serviceType = serviceType;
	}
	public short getServiceVersion() {
		return serviceVersion;
	}
	public void setServiceVersion(short serviceVersion) {
		this.serviceVersion = serviceVersion;
	}
	public int getServiceDataEncoding() {
		return serviceDataEncoding;
	}
	public void setServiceDataEncoding(int serviceDataEncoding) {
		this.serviceDataEncoding = serviceDataEncoding;
	}
	public int getServiceEncryption() {
		return serviceEncryption;
	}
	public void setServiceEncryption(int serviceEncryption) {
		this.serviceEncryption = serviceEncryption;
	}
	public byte[] getServiceData() {
		return serviceData;
	}
	public void setServiceData(byte[] serviceData) {
		this.serviceData = serviceData;
	}
	public int getServiceDataProtocolVersion() {
		return serviceDataProtocolVersion;
	}
	public void setServiceDataProtocolVersion(int serviceDataProtocolVersion) {
		this.serviceDataProtocolVersion = serviceDataProtocolVersion;
	}
	public int getEventId() {
		return eventId;
	}
	public void setEventId(int eventId) {
		this.eventId = eventId;
	}
	public synchronized Date getEventCreationTime() {
		return eventCreationTime;
	}
	public synchronized void setEventCreationTime(Date eventCreationTime) {
		this.eventCreationTime = eventCreationTime;
	}
}
