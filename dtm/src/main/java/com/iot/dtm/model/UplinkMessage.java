package com.iot.dtm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 上行业务报文（proxy.uplink 输入 / platform.uplink 输出）。
 * 原有字段与 ProxyTcp 的 ServiceRequest 对齐；dtm 解码后追加结构化字段。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UplinkMessage {

    private String deviceId;
    private String nodeId;
    private Long eventId;
    /** Gson 序列化 Date 的格式不固定，保留原始 JSON 节点避免解析失败。 */
    private JsonNode eventCreationTime;
    private Integer serviceType;
    private Integer serviceVersion;
    private Integer serviceDataEncoding;
    private Integer serviceDataProtocolVersion;
    private Integer serviceEncryption;
    /** base64 编码的设备原始业务载荷。 */
    private String serviceData;

    private JsonNode serviceDataDecoded;
    private String protocolName;
    private String transactionType;
    private String decodeStatus;
    private String decodeError;

    public DeviceProfile toDeviceProfile() {
        DeviceProfile p = new DeviceProfile();
        p.setDeviceId(deviceId);
        p.setServiceVersion(serviceVersion);
        p.setServiceDataEncoding(serviceDataEncoding);
        p.setServiceDataProtocolVersion(serviceDataProtocolVersion);
        p.setServiceEncryption(serviceEncryption);
        return p;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public JsonNode getEventCreationTime() { return eventCreationTime; }
    public void setEventCreationTime(JsonNode eventCreationTime) { this.eventCreationTime = eventCreationTime; }

    public Integer getServiceType() { return serviceType; }
    public void setServiceType(Integer serviceType) { this.serviceType = serviceType; }

    public Integer getServiceVersion() { return serviceVersion; }
    public void setServiceVersion(Integer serviceVersion) { this.serviceVersion = serviceVersion; }

    public Integer getServiceDataEncoding() { return serviceDataEncoding; }
    public void setServiceDataEncoding(Integer serviceDataEncoding) { this.serviceDataEncoding = serviceDataEncoding; }

    public Integer getServiceDataProtocolVersion() { return serviceDataProtocolVersion; }
    public void setServiceDataProtocolVersion(Integer serviceDataProtocolVersion) {
        this.serviceDataProtocolVersion = serviceDataProtocolVersion;
    }

    public Integer getServiceEncryption() { return serviceEncryption; }
    public void setServiceEncryption(Integer serviceEncryption) { this.serviceEncryption = serviceEncryption; }

    public String getServiceData() { return serviceData; }
    public void setServiceData(String serviceData) { this.serviceData = serviceData; }

    public JsonNode getServiceDataDecoded() { return serviceDataDecoded; }
    public void setServiceDataDecoded(JsonNode serviceDataDecoded) { this.serviceDataDecoded = serviceDataDecoded; }

    public String getProtocolName() { return protocolName; }
    public void setProtocolName(String protocolName) { this.protocolName = protocolName; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getDecodeStatus() { return decodeStatus; }
    public void setDecodeStatus(String decodeStatus) { this.decodeStatus = decodeStatus; }

    public String getDecodeError() { return decodeError; }
    public void setDecodeError(String decodeError) { this.decodeError = decodeError; }
}
