package com.iot.dtm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.config.MqConstants;
import com.iot.dtm.model.DeviceProfile;
import com.iot.dtm.model.DownlinkMessage;
import com.iot.dtm.mq.MessagePublisher;
import com.iot.dtm.profile.DeviceProfileCache;
import com.iot.dtm.protocol.encoder.TransportEncoderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 下行处理：platform.downlink -> 解析 DownlinkMessage -> 合并 DeviceProfile
 * -> 结构化字段编码为二进制 -> 剥离扩展字段 -> 发布 proxy.downlink（等待 confirm）。
 */
@Service
public class DownlinkService extends AbstractEnvelopeService {

    private static final Logger log = LoggerFactory.getLogger(DownlinkService.class);

    @Autowired
    public DownlinkService(DeviceProfileCache profileCache, TransportEncoderManager encoderManager,
                           CodecProperties properties, MessagePublisher publisher) {
        super(profileCache, encoderManager, properties, publisher);
    }

    public Message process(Message message) {
        Message out = doProcess(message);
        publish(out, MqConstants.PROXY_DOWNLINK);
        return out;
    }

    private Message doProcess(Message message) {
        if (!properties.isEnabled()) {
            return message;
        }
        DownlinkMessage downlink;
        try {
            downlink = mapper.readValue(message.getBody(), DownlinkMessage.class);
        } catch (Exception e) {
            throw reject("invalid downlink JSON envelope: " + e.getMessage(), "downlink envelope");
        }
        if (downlink.getServiceType() == null) {
            throw reject("downlink envelope has no numeric serviceType", "downlink envelope");
        }

        DeviceProfile profile = profileCache.merge(downlink.getDeviceId(), downlink.toDeviceProfile());
        JsonNode decoded = downlink.getServiceDataDecoded();
        if (decoded != null && !decoded.isNull()) {
            if (!decoded.isObject()) {
                throw reject("serviceDataDecoded must be a JSON object", "downlink encode");
            }
            byte[] bytes;
            try {
                bytes = encoderManager.encode(profile, downlink.getServiceType(), decoded);
            } catch (RuntimeException e) {
                throw reject("encode serviceDataDecoded failed: " + e.getMessage(), "downlink encode");
            }
            downlink.setServiceData(encodeBase64(bytes));
            log.info("dtm downlink encode: device={} serviceType={} bytes={} -> {}",
                    downlink.getDeviceId(), downlink.getServiceType(), bytes.length,
                    MqConstants.PROXY_DOWNLINK);
        } else if (isBlank(downlink.getServiceData())) {
            // 空载荷协议也必须给出字符串：ProxyTcp 的 base64 适配器不接受 JSON null
            downlink.setServiceData("");
        }
        downlink.setServiceDataDecoded(null);

        try {
            return withBody(message, mapper.writeValueAsBytes(downlink));
        } catch (Exception e) {
            throw reject("failed to serialize downlink envelope: " + e.getMessage(), "downlink serialize");
        }
    }
}
