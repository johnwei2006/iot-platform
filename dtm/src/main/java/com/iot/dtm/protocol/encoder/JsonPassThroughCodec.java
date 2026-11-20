package com.iot.dtm.protocol.encoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.dtm.protocol.CodecException;
import com.iot.dtm.protocol.DecodeResult;
import com.iot.dtm.protocol.ProtocolSpec;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** serviceDataEncoding=1：serviceData 本身就是 UTF-8 JSON 文本，不做二进制 schema 编解码。 */
@Component
public class JsonPassThroughCodec {

    private final ObjectMapper mapper = new ObjectMapper();

    public DecodeResult decode(ProtocolSpec spec, byte[] data) {
        try {
            JsonNode parsed = mapper.readTree(new String(data == null ? new byte[0] : data,
                    StandardCharsets.UTF_8));
            if (parsed == null || !parsed.isObject()) {
                return DecodeResult.failed(spec, "JSON serviceData is not an object");
            }
            return DecodeResult.ok(spec, (ObjectNode) parsed);
        } catch (Exception e) {
            return DecodeResult.failed(spec, "invalid JSON serviceData: " + e.getMessage());
        }
    }

    public byte[] encode(JsonNode decoded) {
        if (decoded == null || !decoded.isObject()) {
            throw new CodecException("serviceDataDecoded must be a JSON object");
        }
        try {
            return mapper.writeValueAsBytes(decoded);
        } catch (Exception e) {
            throw new CodecException("failed to serialize JSON serviceData: " + e.getMessage(), e);
        }
    }
}
