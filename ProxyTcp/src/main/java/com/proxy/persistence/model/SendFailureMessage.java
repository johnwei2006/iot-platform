package com.proxy.persistence.model;

public class SendFailureMessage {

	private String deviceId;

	private long eventId;

	private int serviceType;

	private int sourceServiceType;

	private int serviceVersion;

	private long messageId;

	private String ip;

	private int port;

	private int errorCode;
	
	private String errorMessage;

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

	public final long getEventId() {
		return eventId;
	}

	public final void setEventId(long eventId) {
		this.eventId = eventId;
	}

	public final int getServiceType() {
		return serviceType;
	}

	public final void setServiceType(int serviceType) {
		this.serviceType = serviceType;
	}

	public final int getServiceVersion() {
		return serviceVersion;
	}

	public final void setServiceVersion(int serviceVersion) {
		this.serviceVersion = serviceVersion;
	}

	public final String getIp() {
		return ip;
	}

	public final void setIp(String ip) {
		this.ip = ip;
	}

	public final int getPort() {
		return port;
	}

	public final void setPort(int port) {
		this.port = port;
	}

	public final int getSourceServiceType() {
		return sourceServiceType;
	}

	public final void setSourceServiceType(int sourceServiceType) {
		this.sourceServiceType = sourceServiceType;
	}

	public final int getErrorCode() {
		return errorCode;
	}

	public final void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
