package com.iot.dtm.protocol.encoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.model.DeviceProfile;
import com.iot.dtm.protocol.BinaryReader;
import com.iot.dtm.protocol.BinaryWriter;
import com.iot.dtm.protocol.CodecException;
import com.iot.dtm.protocol.DecodeResult;
import com.iot.dtm.protocol.ProtocolRegistry;
import com.iot.dtm.protocol.ProtocolSpec;
import com.iot.dtm.protocol.ServiceType;

import java.nio.charset.StandardCharsets;

/**
 * 基于 protocol-schemas.json 的通用编解码实现。
 * 37 个协议 Encoder 只传入 ServiceType，公共逻辑全部收敛在这里。
 */
public abstract class AbstractSchemaEncoder implements TransportEncoder {

    public static final int ENCODING_JSON = 1;
    public static final int ENCODING_BINARY = 2;
    public static final int PROTOCOL_VERSION_1 = 1;
    public static final int ENCRYPTION_NONE = 0;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ServiceType serviceType;
    private final ProtocolSpec spec;
    protected final CodecProperties properties;

    protected AbstractSchemaEncoder(ServiceType serviceType, ProtocolRegistry registry,
                                   CodecProperties properties) {
        this.serviceType = serviceType;
        this.spec = registry.get(serviceType);
        this.properties = properties;
        if (spec == null) {
            throw new IllegalStateException("no protocol schema for " + serviceType.name());
        }
    }

    @Override
    public ServiceType serviceType() {
        return serviceType;
    }

    protected ProtocolSpec spec() {
        return spec;
    }

    @Override
    public DecodeResult decode(DeviceProfile profile, byte[] serviceData) {
        int encoding = profile == null ? ENCODING_BINARY : profile.effectiveServiceDataEncoding();
        int protocolVersion = profile == null ? PROTOCOL_VERSION_1
                : profile.effectiveServiceDataProtocolVersion();
        int encryption = profile == null ? ENCRYPTION_NONE : profile.effectiveServiceEncryption();

        String unsupported = unsupportedReason(encoding, protocolVersion, encryption);
        if (unsupported != null) {
            return DecodeResult.unsupported(spec, unsupported);
        }

        byte[] data = serviceData == null ? new byte[0] : serviceData;
        if (data.length > properties.getMaxPayloadBytes()) {
            return DecodeResult.failed(spec, "serviceData length " + data.length
                    + " exceeds limit " + properties.getMaxPayloadBytes());
        }

        try {
            if (encoding == ENCODING_JSON) {
                JsonNode parsed = MAPPER.readTree(new String(data, StandardCharsets.UTF_8));
                if (parsed == null || !parsed.isObject()) {
                    return DecodeResult.failed(spec, "JSON serviceData is not an object");
                }
                return DecodeResult.ok(spec, (ObjectNode) parsed);
            }
            if (spec.isEmpty()) {
                if (data.length > 0) {
                    return DecodeResult.failed(spec, "expected empty serviceData but got "
                            + data.length + " bytes");
                }
                return DecodeResult.empty(spec);
            }
            BinaryReader reader = new BinaryReader(data, properties.getMaxRepeatItems());
            ObjectNode decoded = reader.read(spec.getFields());
            if (reader.remaining() > 0) {
                decoded.put("unparsedBytes", reader.remainingHex());
            }
            return DecodeResult.ok(spec, decoded);
        } catch (CodecException e) {
            return DecodeResult.failed(spec, e.getMessage());
        } catch (Exception e) {
            return DecodeResult.failed(spec, "decode error: " + e.getMessage());
        }
    }

    @Override
    public byte[] encode(DeviceProfile profile, JsonNode decoded) {
        int encoding = profile == null ? ENCODING_BINARY : profile.effectiveServiceDataEncoding();
        int protocolVersion = profile == null ? PROTOCOL_VERSION_1
                : profile.effectiveServiceDataProtocolVersion();
        int encryption = profile == null ? ENCRYPTION_NONE : profile.effectiveServiceEncryption();

        String unsupported = unsupportedReason(encoding, protocolVersion, encryption);
        if (unsupported != null) {
            throw new CodecException(unsupported);
        }
        if (decoded == null || !decoded.isObject()) {
            throw new CodecException("serviceDataDecoded must be a JSON object");
        }
        try {
            if (encoding == ENCODING_JSON) {
                return MAPPER.writeValueAsBytes(decoded);
            }
            if (spec.isEmpty()) {
                return new byte[0];
            }
            BinaryWriter writer = new BinaryWriter();
            writer.write(spec.getFields(), decoded);
            byte[] bytes = writer.toByteArray();
            if (bytes.length > properties.getMaxPayloadBytes()) {
                throw new CodecException("encoded serviceData length " + bytes.length
                        + " exceeds limit " + properties.getMaxPayloadBytes());
            }
            return bytes;
        } catch (CodecException e) {
            throw e;
        } catch (Exception e) {
            throw new CodecException("encode error: " + e.getMessage(), e);
        }
    }

    private String unsupportedReason(int encoding, int protocolVersion, int encryption) {
        if (spec.isUnsupported()) {
            return "protocol " + spec.getHex() + " has no field schema in Protocal5.0.xlsx";
        }
        if (encryption != ENCRYPTION_NONE) {
            return "encrypted serviceData is not supported (serviceEncryption=" + encryption + ")";
        }
        if (protocolVersion != PROTOCOL_VERSION_1) {
            return "unsupported serviceDataProtocolVersion=" + protocolVersion;
        }
        if (encoding != ENCODING_BINARY && encoding != ENCODING_JSON) {
            return "unsupported serviceDataEncoding=" + encoding;
        }
        return null;
    }
}
