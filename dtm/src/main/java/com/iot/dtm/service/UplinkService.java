package com.iot.dtm.service;

import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.config.MqConstants;
import com.iot.dtm.model.DeviceProfile;
import com.iot.dtm.model.UplinkMessage;
import com.iot.dtm.mq.MessagePublisher;
import com.iot.dtm.profile.DeviceProfileCache;
import com.iot.dtm.protocol.DecodeResult;
import com.iot.dtm.protocol.encoder.TransportEncoderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上行处理：proxy.uplink -> 解析 UplinkMessage -> 合并 DeviceProfile -> 解码 serviceData
 * -> 追加结构化字段 -> 发布 platform.uplink（等待 confirm）。
 */
@Service
public class UplinkService extends AbstractEnvelopeService {

    private static final Logger log = LoggerFactory.getLogger(UplinkService.class);

    private final Set<Integer> warnedUnknown = ConcurrentHashMap.newKeySet();

    @Autowired
    public UplinkService(DeviceProfileCache profileCache, TransportEncoderManager encoderManager,
                         CodecProperties properties, MessagePublisher publisher) {
        super(profileCache, encoderManager, properties, publisher);
    }

    public Message process(Message message) {
        Message out = doProcess(message);
        publish(out, MqConstants.PLATFORM_UPLINK);
        return out;
    }

    private Message doProcess(Message message) {
        if (!properties.isEnabled()) {
            return message;
        }
        UplinkMessage uplink;
        try {
            uplink = mapper.readValue(message.getBody(), UplinkMessage.class);
        } catch (Exception e) {
            return fail(message, MqConstants.H_DECODE_STATUS, MqConstants.H_DECODE_ERROR,
                    "invalid uplink JSON envelope: " + e.getMessage(), "uplink envelope");
        }
        if (uplink.getServiceType() == null) {
            return fail(message, MqConstants.H_DECODE_STATUS, MqConstants.H_DECODE_ERROR,
                    "uplink envelope has no numeric serviceType", "uplink envelope");
        }

        DeviceProfile profile = profileCache.merge(uplink.getDeviceId(), uplink.toDeviceProfile());
        byte[] data;
        try {
            data = decodeBase64(uplink.getServiceData());
        } catch (IllegalArgumentException e) {
            return fail(message, MqConstants.H_DECODE_STATUS, MqConstants.H_DECODE_ERROR,
                    "invalid base64 serviceData: " + e.getMessage(), "uplink envelope");
        }

        DecodeResult result = encoderManager.decode(profile, uplink.getServiceType(), data);
        uplink.setProtocolName(result.getProtocolName());
        uplink.setTransactionType(result.getTransactionType());
        uplink.setDecodeStatus(result.getStatus().name());
        uplink.setDecodeError(result.getReason());
        if (result.hasDecoded() || result.getStatus() == DecodeResult.Status.EMPTY) {
            uplink.setServiceDataDecoded(result.getDecoded());
        } else {
            uplink.setServiceDataDecoded(null);
        }
        logDecode(uplink, data.length, result);

        try {
            return withBody(message, mapper.writeValueAsBytes(uplink));
        } catch (Exception e) {
            return fail(message, MqConstants.H_ENCODE_STATUS, MqConstants.H_ENCODE_ERROR,
                    "failed to serialize uplink envelope: " + e.getMessage(), "uplink serialize");
        }
    }

    private void logDecode(UplinkMessage message, int length, DecodeResult result) {
        if (result.getStatus() == DecodeResult.Status.OK
                || result.getStatus() == DecodeResult.Status.EMPTY) {
            log.info("dtm uplink decode: device={} serviceType={} protocol={} status={} bytes={}",
                    message.getDeviceId(), message.getServiceType(), result.getProtocolName(),
                    result.getStatus(), length);
            if (log.isDebugEnabled()) {
                log.debug("dtm uplink decoded fields: {}", result.getDecoded());
            }
            return;
        }
        if (result.getStatus() == DecodeResult.Status.FAILED) {
            log.warn("dtm uplink decode failed: device={} serviceType={} bytes={} reason={}",
                    message.getDeviceId(), message.getServiceType(), length, result.getReason());
        } else if (warnedUnknown.add(message.getServiceType())) {
            log.warn("dtm uplink unsupported: serviceType=0x{} protocol={} reason={} (further messages suppressed)",
                    Integer.toHexString(message.getServiceType()), result.getProtocolName(),
                    result.getReason());
        }
    }
}
