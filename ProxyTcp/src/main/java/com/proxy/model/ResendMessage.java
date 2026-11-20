package com.proxy.model;

public class ResendMessage {
	private String deviceId;
	private String messageByteHex;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getMessageByteHex() {
		return messageByteHex;
	}

	public void setMessageByteHex(String messageByteHex) {
		this.messageByteHex = messageByteHex;
	}

}
