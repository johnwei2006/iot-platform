package com.iot.dtm.protocol.encoder.impl;

import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.protocol.ProtocolRegistry;
import com.iot.dtm.protocol.ServiceType;
import com.iot.dtm.protocol.encoder.AbstractSchemaEncoder;
import org.springframework.stereotype.Component;

/** GATEWAY_UPGRADE 协议编码器（字段定义见 protocol-schemas.json）。 */
@Component
public class GatewayUpgradeEncoder extends AbstractSchemaEncoder {

    public GatewayUpgradeEncoder(ProtocolRegistry registry, CodecProperties properties) {
        super(ServiceType.GATEWAY_UPGRADE, registry, properties);
    }
}
