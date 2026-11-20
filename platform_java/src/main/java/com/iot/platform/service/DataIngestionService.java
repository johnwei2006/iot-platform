package com.iot.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.mq.UplinkProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 上行数据接入兼容入口（deprecated facade）。
 *
 * <p>MQ 监听已迁移至 {@link com.iot.platform.mq.UplinkMessageListener}，
 * 业务处理由 {@link UplinkProcessingService} 编排各 UplinkHandler 完成。
 * 此类仅保留 onUplink(Message) 供既有调用方平滑迁移，后续版本将移除。
 */
@Service
public class DataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DataIngestionService.class);

    private final ObjectMapper objectMapper;
    private final UplinkProcessingService processingService;

    @Autowired
    public DataIngestionService(ObjectMapper objectMapper, UplinkProcessingService processingService) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }

    public void onUplink(Message message) {
        try {
            UplinkMessage up = objectMapper.readValue(message.getBody(), UplinkMessage.class);
            processingService.process(up);
        } catch (Exception e) {
            log.error("failed to ingest uplink", e);
            throw new RuntimeException(e);
        }
    }
}
