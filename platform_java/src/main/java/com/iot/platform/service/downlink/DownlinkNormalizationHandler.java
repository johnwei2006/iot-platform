package com.iot.platform.service.downlink;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.platform.mq.DownlinkMessage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 下行步骤 2：载荷规范化。
 *
 * <p>无结构化字段且未提供 base64 serviceData 时，置为空串，兼容 dtm 编解码器
 * 对空载荷的既有语义（序列化时仍会被 NON_NULL 策略忽略）。
 */
@Component
@Order(2)
public class DownlinkNormalizationHandler implements DownlinkHandler {

    @Override
    public String name() {
        return "downlinkNormalization";
    }

    @Override
    public void handle(DownlinkMessage message) {
        JsonNode decoded = message.getServiceDataDecoded();
        boolean hasStructured = decoded != null && !decoded.isNull();
        boolean hasBase64 = message.getServiceData() != null && !message.getServiceData().isEmpty();
        if (!hasStructured && !hasBase64) {
            message.setServiceData("");
        }
    }
}
