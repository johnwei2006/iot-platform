package com.iot.platform.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iot.platform.common.JsonTypeHandlers;

import java.util.Date;
import java.util.Map;

@TableName(value = "iot_data_record", autoResultMap = true)
public class DataRecord {

    @TableId(value = "id", type = IdType.NONE)
    private String id;
    @TableField("device_id")
    private String deviceId;
    @TableField("service_type")
    private int serviceType;
    @TableField("service_version")
    private int serviceVersion;
    /** 业务数据原始字节的 base64 */
    @TableField("service_data")
    private String serviceData;
    @TableField("event_id")
    private long eventId;
    @TableField("created_at")
    private Date createdAt;
    /** dtm 解码出的结构化业务字段（JSON 列） */
    @TableField(value = "service_data_decoded", typeHandler = JsonTypeHandlers.MapObjectTypeHandler.class)
    private Map<String, Object> serviceDataDecoded;
    @TableField("protocol_name")
    private String protocolName;
    @TableField("transaction_type")
    private String transactionType;
    @TableField("decode_status")
    private String decodeStatus;
    @TableField("decode_error")
    private String decodeError;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public int getServiceType() { return serviceType; }
    public void setServiceType(int serviceType) { this.serviceType = serviceType; }
    public int getServiceVersion() { return serviceVersion; }
    public void setServiceVersion(int serviceVersion) { this.serviceVersion = serviceVersion; }
    public String getServiceData() { return serviceData; }
    public void setServiceData(String serviceData) { this.serviceData = serviceData; }
    public long getEventId() { return eventId; }
    public void setEventId(long eventId) { this.eventId = eventId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Map<String, Object> getServiceDataDecoded() { return serviceDataDecoded; }
    public void setServiceDataDecoded(Map<String, Object> serviceDataDecoded) { this.serviceDataDecoded = serviceDataDecoded; }
    public String getProtocolName() { return protocolName; }
    public void setProtocolName(String protocolName) { this.protocolName = protocolName; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getDecodeStatus() { return decodeStatus; }
    public void setDecodeStatus(String decodeStatus) { this.decodeStatus = decodeStatus; }
    public String getDecodeError() { return decodeError; }
    public void setDecodeError(String decodeError) { this.decodeError = decodeError; }
}
