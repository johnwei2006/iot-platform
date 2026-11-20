package com.proxy.model;

public class ServiceResponse {
	private String 	deviceId;
	private int 	serviceType;
	private short 	serviceVersion;
	private long 	eventId;
	private int 	status;
	//errorCode will be used when status is STATUS_ERROR(400)
	private int 	errorCode;
	private String 	message;

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public long getEventId() {
		return eventId;
	}

	public void setEventId(long eventId) {
		this.eventId = eventId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
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

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
}
