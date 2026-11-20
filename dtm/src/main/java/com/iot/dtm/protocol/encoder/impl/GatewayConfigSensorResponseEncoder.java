package com.iot.dtm.protocol.encoder.impl;

import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.protocol.ProtocolRegistry;
import com.iot.dtm.protocol.ServiceType;
import com.iot.dtm.protocol.encoder.AbstractSchemaEncoder;
import org.springframework.stereotype.Component;

/** GATEWAY_CONFIG_SENSOR_RESPONSE 协议编码器（字段定义见 protocol-schemas.json）。 */
@Component
public class GatewayConfigSensorResponseEncoder extends AbstractSchemaEncoder {

    public GatewayConfigSensorResponseEncoder(ProtocolRegistry registry, CodecProperties properties) {
        super(ServiceType.GATEWAY_CONFIG_SENSOR_RESPONSE, registry, properties);
    }
}
