package com.iot.dtm.protocol.encoder;

import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.protocol.ProtocolRegistry;
import com.iot.dtm.protocol.ServiceType;

/**
 * 通用 schema Encoder。生产环境使用 impl 包下 37 个具体子类；
 * 该类用于测试或需要按 ServiceType 动态构造编码器的场景。
 */
public class SchemaTransportEncoder extends AbstractSchemaEncoder {

    public SchemaTransportEncoder(ServiceType serviceType, ProtocolRegistry registry,
                                 CodecProperties properties) {
        super(serviceType, registry, properties);
    }
}
