package com.iot.platform.service.uplink;

import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.service.converter.UplinkBusinessDispatcher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 旁路步骤 4：业务转换分派。
 *
 * <p>委托 {@link UplinkBusinessDispatcher}，后者按 serviceType 分派到各
 * UplinkBusinessConverter（传感器配置登记、0x18 读数 -> TDengine 归档）。
 * 处理器本身是 best-effort：不存在分派器或执行失败都由编排层降级处理，
 * 不影响上行 ACK。
 */
@Component
@Order(4)
public class BusinessConversionHandler implements UplinkHandler {

    private final UplinkBusinessDispatcher dispatcher;

    @Autowired
    public BusinessConversionHandler(ObjectProvider<UplinkBusinessDispatcher> provider) {
        this.dispatcher = provider == null ? null : provider.getIfAvailable();
    }

    @Override
    public String name() {
        return "businessConversion";
    }

    @Override
    public boolean critical() {
        return false;
    }

    @Override
    public void handle(UplinkMessage message) {
        if (dispatcher != null) {
            dispatcher.dispatch(message);
        }
    }
}
