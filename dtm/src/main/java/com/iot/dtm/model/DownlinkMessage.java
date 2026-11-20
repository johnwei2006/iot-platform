package com.iot.dtm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 下行业务报文（platform.downlink 输入 / proxy.downlink 输出）。
 * 可携带结构化 serviceDataDecoded（dtm 编码为二进制）或旧 base64 serviceData（直接透传）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DownlinkMessage {

    private String deviceId;
    private String nodeId;
    private Long eventId;
    private JsonNode eventCreationTime;
    private Integer serviceType;
    private Integer serviceVersion;
    private Integer serviceDataEncoding;
    private Integer serviceDataProtocolVersion;
    private Integer serviceEncryption;
    private String serviceData;

    /** 结构化业务字段；存在且为对象时由 dtm 编码回二进制。 */
    private JsonNode serviceDataDecoded;

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
}
