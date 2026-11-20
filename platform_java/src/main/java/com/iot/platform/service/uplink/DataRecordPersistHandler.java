package com.iot.platform.service.uplink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.model.DataRecord;
import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.repository.DataRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * 主链路步骤 1：把上行信封映射为 DataRecord 并持久化到 MongoDB。
 */
@Component
@Order(1)
public class DataRecordPersistHandler implements UplinkHandler {

    private final ObjectMapper objectMapper;
    private final DataRecordRepository dataRecordRepository;

    @Autowired
    public DataRecordPersistHandler(ObjectMapper objectMapper, DataRecordRepository dataRecordRepository) {
        this.objectMapper = objectMapper;
        this.dataRecordRepository = dataRecordRepository;
    }

    @Override
    public String name() {
        return "dataRecordPersist";
    }

    @Override
    public boolean critical() {
        return true;
    }

    @Override
    public void handle(UplinkMessage message) {
        DataRecord rec = new DataRecord();
        rec.setDeviceId(message.getDeviceId());
        rec.setServiceType(message.getServiceType());
        rec.setServiceVersion(message.getServiceVersion());
        rec.setServiceData(message.getServiceData());
        rec.setEventId(message.getEventId());
        rec.setCreatedAt(new Date());
        rec.setServiceDataDecoded(toMap(message.getServiceDataDecoded()));
        rec.setProtocolName(message.getProtocolName());
        rec.setTransactionType(message.getTransactionType());
        rec.setDecodeStatus(message.getDecodeStatus());
        rec.setDecodeError(message.getDecodeError());
        dataRecordRepository.save(rec);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return objectMapper.convertValue(node, Map.class);
    }
}
