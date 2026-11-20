package com.proxy.test;

import com.proxy.test.protocol.MockDecodeResult;

/**
 * 模拟设备接收并解析后的一帧下行报文。
 *
 * <p>同时保留传输层字段、原始 serviceData 与业务解码结果，便于测试断言和人工排查。
 */
public class MockTransportMessage {

    private int transportDataEncoding;
    private int transportVersion;
    private int transportDataLength;

    private String deviceId;
    private String nodeId;
    private String messageCreationTime;
    private int messageId;
    private int serviceType;
    private int serviceVersion;
    private int serviceDataEncoding;
    private int serviceDataProtocolVersion;
    private int serviceDataEncryption;
    private int eventId;

    private byte[] serviceData;
    private int frameLength;
    private MockDecodeResult business;

    public int getTransportDataEncoding() {
        return transportDataEncoding;
    }

    public void setTransportDataEncoding(int transportDataEncoding) {
        this.transportDataEncoding = transportDataEncoding;
    }

    public int getTransportVersion() {
        return transportVersion;
    }

    public void setTransportVersion(int transportVersion) {
        this.transportVersion = transportVersion;
    }

    public int getTransportDataLength() {
        return transportDataLength;
    }

    public void setTransportDataLength(int transportDataLength) {
        this.transportDataLength = transportDataLength;
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

    public String getMessageCreationTime() {
        return messageCreationTime;
    }

    public void setMessageCreationTime(String messageCreationTime) {
        this.messageCreationTime = messageCreationTime;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getServiceType() {
        return serviceType;
    }

    public void setServiceType(int serviceType) {
        this.serviceType = serviceType;
    }

    public int getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(int serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public int getServiceDataEncoding() {
        return serviceDataEncoding;
    }

    public void setServiceDataEncoding(int serviceDataEncoding) {
        this.serviceDataEncoding = serviceDataEncoding;
    }

    public int getServiceDataProtocolVersion() {
        return serviceDataProtocolVersion;
    }

    public void setServiceDataProtocolVersion(int serviceDataProtocolVersion) {
        this.serviceDataProtocolVersion = serviceDataProtocolVersion;
    }

    public int getServiceDataEncryption() {
        return serviceDataEncryption;
    }

    public void setServiceDataEncryption(int serviceDataEncryption) {
        this.serviceDataEncryption = serviceDataEncryption;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public byte[] getServiceData() {
        return serviceData;
    }

    public void setServiceData(byte[] serviceData) {
        this.serviceData = serviceData;
    }

    public int getFrameLength() {
        return frameLength;
    }

    public void setFrameLength(int frameLength) {
        this.frameLength = frameLength;
    }

    public MockDecodeResult getBusiness() {
        return business;
    }

    public void setBusiness(MockDecodeResult business) {
        this.business = business;
    }
}
