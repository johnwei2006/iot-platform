package com.proxy.model;

import java.util.Date;


public class CommonTransportMessage implements TransportMessage{
	//deviceId(11) + nodeId(11) + messageCreation_time(6) + messageId(4) + serviceType(1) + serviceVersion(1) + serviceDataEncoding(1) + serviceDataProtocolVersion(1) + serviceDataEncryption(1) + eventId(4) + token(10)
	public static short TRANSPORT_PART_FIX_LENGTH 		= 51;

	// 网关认证 token 长度（10 字节二进制随机数），位于传输层定长段末尾（偏移 41..50）
	public static final int TOKEN_LENGTH 				= 10;

	private MessageHeader 	header;
	private String 			deviceId;
	private String 			nodeId;
	private Date 			messageCreationTime;
	private int 			messageId;
	private int 			serviceType;
	private short 			serviceVersion;
	private int 			ServiceDataEncoding = 0;
	private int 			serviceDataProtocolVersion;
	private int 			serviceDataEncryption;
	private int 			eventId;
	private byte[] 			token;
	private byte[] 			serviceData;
	private int 			packageSize;
	private int 			serviceDataSize;
	
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
		return ServiceDataEncoding;
	}
	public void setServiceDataEncoding(int serviceDataEncoding) {
		ServiceDataEncoding = serviceDataEncoding;
	}
	public int getServiceDataEncryption() {
		return serviceDataEncryption;
	}
	public void setServiceDataEncryption(int serviceDataEncryption) {
		this.serviceDataEncryption = serviceDataEncryption;
	}
	public Date getMessageCreationTime() {
		return messageCreationTime;
	}
	public void setMessageCreationTime(Date messageCreationTime) {
		this.messageCreationTime = messageCreationTime;
	}
	public int getMessageId() {
		return messageId;
	}
	public void setMessageId(int m_messageId) {
		this.messageId = m_messageId;
	}
	public byte[] getServiceData() {
		return serviceData;
	}
	public void setServiceData(byte[] serviceData) {
		this.serviceData = serviceData;
	}
	public MessageHeader getHeader() {
		return header;
	}
	public void setHeader(MessageHeader header) {
		this.header = header;
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
	public byte[] getToken() {
		return token;
	}
	public void setToken(byte[] token) {
		this.token = token;
	}
	public int getPackageSize() {
		return packageSize;
	}
	public void setPackageSize(int packageSize) {
		this.packageSize = packageSize;
	}
	public int getServiceDataSize() {
		return serviceDataSize;
	}
	public void setServiceDataSize(int serviceDataSize) {
		this.serviceDataSize = serviceDataSize;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
}
