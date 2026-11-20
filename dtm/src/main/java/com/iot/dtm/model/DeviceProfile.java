package com.iot.dtm.model;

/**
 * 设备画像：决定某条报文该用哪套传输编码规则。
 * 字段全部使用包装类型，便于区分「报文未携带」与「值为 0」。
 */
public class DeviceProfile {

    private String deviceId;
    private Integer serviceVersion;
    private Integer serviceDataEncoding;
    private Integer serviceDataProtocolVersion;
    private Integer serviceEncryption;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

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

    /** 默认 2 = BINARY；1 = JSON 文本。 */
    public int effectiveServiceDataEncoding() {
        return serviceDataEncoding == null ? 2 : serviceDataEncoding;
    }

    /** 当前仅支持协议版本 1。 */
    public int effectiveServiceDataProtocolVersion() {
        return serviceDataProtocolVersion == null ? 1 : serviceDataProtocolVersion;
    }

    /** 0 = 不加密。 */
    public int effectiveServiceEncryption() {
        return serviceEncryption == null ? 0 : serviceEncryption;
    }

    public DeviceProfile copy() {
        DeviceProfile p = new DeviceProfile();
        p.deviceId = deviceId;
        p.serviceVersion = serviceVersion;
        p.serviceDataEncoding = serviceDataEncoding;
        p.serviceDataProtocolVersion = serviceDataProtocolVersion;
        p.serviceEncryption = serviceEncryption;
        return p;
    }

    @Override
    public String toString() {
        return "DeviceProfile{deviceId='" + deviceId + "', serviceVersion=" + serviceVersion
                + ", encoding=" + serviceDataEncoding + ", protocolVersion=" + serviceDataProtocolVersion
                + ", encryption=" + serviceEncryption + '}';
    }
}
