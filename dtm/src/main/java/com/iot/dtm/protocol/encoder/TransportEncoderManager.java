package com.iot.dtm.protocol.encoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.dtm.model.DeviceProfile;
import com.iot.dtm.protocol.CodecException;
import com.iot.dtm.protocol.DecodeResult;
import com.iot.dtm.protocol.ProtocolRegistry;
import com.iot.dtm.protocol.ProtocolSpec;
import com.iot.dtm.protocol.ServiceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 传输编码器管理器：按 serviceType 选择 37 个协议 Encoder，
 * 并按 DeviceProfile 校验编码方式、协议版本与加密方式。
 */
@Component
public class TransportEncoderManager {

    private final Map<Integer, TransportEncoder> byCode;
    private final ProtocolRegistry registry;
    private final JsonPassThroughCodec jsonCodec;

    @Autowired
    public TransportEncoderManager(List<TransportEncoder> encoders, ProtocolRegistry registry,
                                   JsonPassThroughCodec jsonCodec) {
        this.registry = registry;
        this.jsonCodec = jsonCodec;
        Map<Integer, TransportEncoder> map = new HashMap<Integer, TransportEncoder>();
        if (encoders != null) {
            for (TransportEncoder encoder : encoders) {
                ServiceType type = encoder.serviceType();
                if (type == null) {
                    throw new IllegalStateException("encoder without serviceType: "
                            + encoder.getClass().getName());
                }
                if (map.put(type.getCode(), encoder) != null) {
                    throw new IllegalStateException("duplicate encoder for " + type.name());
                }
            }
        }
        for (ServiceType type : ServiceType.values()) {
            if (!map.containsKey(type.getCode())) {
                throw new IllegalStateException("missing encoder for " + type.name()
                        + " (0x" + Integer.toHexString(type.getCode()) + ")");
            }
        }
        if (map.size() != ServiceType.values().length) {
            throw new IllegalStateException("registered " + map.size() + " encoders but ServiceType has "
                    + ServiceType.values().length);
        }
        this.byCode = Collections.unmodifiableMap(map);
    }

    public DecodeResult decode(DeviceProfile profile, int serviceType, byte[] serviceData) {
        ServiceType type = ServiceType.of(serviceType);
        ProtocolSpec spec = type == null ? null : registry.get(type);
        if (spec == null) {
            return DecodeResult.unsupported(null, "unknown serviceType 0x"
                    + Integer.toHexString(serviceType));
        }
        String reason = validateProfile(spec, profile);
        if (reason != null) {
            return DecodeResult.unsupported(spec, reason);
        }
        if (profile != null && profile.effectiveServiceDataEncoding() == AbstractSchemaEncoder.ENCODING_JSON) {
            return jsonCodec.decode(spec, serviceData);
        }
        TransportEncoder encoder = byCode.get(serviceType);
        if (encoder == null) {
            return DecodeResult.unsupported(spec, "no encoder registered for 0x"
                    + Integer.toHexString(serviceType));
        }
        return encoder.decode(profile, serviceData);
    }

    public byte[] encode(DeviceProfile profile, int serviceType, JsonNode decoded) {
        ServiceType type = ServiceType.of(serviceType);
        ProtocolSpec spec = type == null ? null : registry.get(type);
        if (spec == null) {
            throw new CodecException("unknown serviceType 0x" + Integer.toHexString(serviceType));
        }
        String reason = validateProfile(spec, profile);
        if (reason != null) {
            throw new CodecException(reason);
        }
        if (profile != null && profile.effectiveServiceDataEncoding() == AbstractSchemaEncoder.ENCODING_JSON) {
            return jsonCodec.encode(decoded);
        }
        TransportEncoder encoder = byCode.get(serviceType);
        if (encoder == null) {
            throw new CodecException("no encoder registered for 0x" + Integer.toHexString(serviceType));
        }
        return encoder.encode(profile, decoded);
    }

    public TransportEncoder getEncoder(int serviceType) {
        return byCode.get(serviceType);
    }

    public int size() {
        return byCode.size();
    }

    private String validateProfile(ProtocolSpec spec, DeviceProfile profile) {
        int encoding = profile == null ? AbstractSchemaEncoder.ENCODING_BINARY
                : profile.effectiveServiceDataEncoding();
        int protocolVersion = profile == null ? AbstractSchemaEncoder.PROTOCOL_VERSION_1
                : profile.effectiveServiceDataProtocolVersion();
        int encryption = profile == null ? AbstractSchemaEncoder.ENCRYPTION_NONE
                : profile.effectiveServiceEncryption();
        if (spec.isUnsupported()) {
            return "protocol " + spec.getHex() + " has no field schema in Protocal5.0.xlsx";
        }
        if (encryption != AbstractSchemaEncoder.ENCRYPTION_NONE) {
            return "encrypted serviceData is not supported (serviceEncryption=" + encryption + ")";
        }
        if (protocolVersion != AbstractSchemaEncoder.PROTOCOL_VERSION_1) {
            return "unsupported serviceDataProtocolVersion=" + protocolVersion;
        }
        if (encoding != AbstractSchemaEncoder.ENCODING_BINARY
                && encoding != AbstractSchemaEncoder.ENCODING_JSON) {
            return "unsupported serviceDataEncoding=" + encoding;
        }
        return null;
    }
}
