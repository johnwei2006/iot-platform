package com.iot.platform.service.downlink;

import com.iot.platform.mq.DownlinkMessage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 下行步骤 1：设备标识校验。
 */
@Component
@Order(1)
public class DownlinkValidationHandler implements DownlinkHandler {

    @Override
    public String name() {
        return "downlinkValidation";
    }

    @Override
    public void handle(DownlinkMessage message) {
        if (message == null || message.getDeviceId() == null || message.getDeviceId().trim().isEmpty()) {
            throw new IllegalArgumentException("deviceId is required for downlink");
        }
    }
}
