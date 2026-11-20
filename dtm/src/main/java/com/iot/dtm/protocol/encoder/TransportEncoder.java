package com.iot.dtm.protocol.encoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.dtm.model.DeviceProfile;
import com.iot.dtm.protocol.DecodeResult;
import com.iot.dtm.protocol.ServiceType;

/**
 * 单个 serviceType 的业务载荷编解码器。
 * 实现类只负责“选择协议 + 委托 schema”；字段定义统一来自 protocol-schemas.json。
 */
public interface TransportEncoder {

    ServiceType serviceType();

    DecodeResult decode(DeviceProfile profile, byte[] serviceData);

    byte[] encode(DeviceProfile profile, JsonNode decoded);
}
