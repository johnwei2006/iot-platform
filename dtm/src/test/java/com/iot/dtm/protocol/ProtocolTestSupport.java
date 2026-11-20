package com.iot.dtm.protocol;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.dtm.config.CodecProperties;
import com.iot.dtm.model.DeviceProfile;
import com.iot.dtm.protocol.ProtocolSpec.FieldSpec;
import com.iot.dtm.protocol.encoder.JsonPassThroughCodec;
import com.iot.dtm.protocol.encoder.SchemaTransportEncoder;
import com.iot.dtm.protocol.encoder.TransportEncoder;
import com.iot.dtm.protocol.encoder.TransportEncoderManager;

import java.util.ArrayList;
import java.util.List;

/** 测试用：构造 schema 合法的合成结构化对象，以及覆盖全部 ServiceType 的编码器管理器。 */
public final class ProtocolTestSupport {

    private static final JsonNodeFactory NF = JsonNodeFactory.instance;

    private ProtocolTestSupport() {
    }

    public static TransportEncoderManager manager(ProtocolRegistry registry) {
        return manager(registry, new CodecProperties());
    }

    public static TransportEncoderManager manager(ProtocolRegistry registry, CodecProperties properties) {
        List<TransportEncoder> encoders = new ArrayList<TransportEncoder>();
        for (ServiceType type : ServiceType.values()) {
            encoders.add(new SchemaTransportEncoder(type, registry, properties));
        }
        return new TransportEncoderManager(encoders, registry, new JsonPassThroughCodec());
    }

    public static DeviceProfile profile(int encoding, int protocolVersion, int encryption) {
        DeviceProfile profile = new DeviceProfile();
        profile.setDeviceId("TEST-DEVICE");
        profile.setServiceVersion(1);
        profile.setServiceDataEncoding(encoding);
        profile.setServiceDataProtocolVersion(protocolVersion);
        profile.setServiceEncryption(encryption);
        return profile;
    }

    public static ObjectNode sample(List<FieldSpec> fields) {
        ObjectNode node = NF.objectNode();
        for (FieldSpec f : fields) {
            node.set(f.getName(), sampleValue(f));
        }
        return node;
    }

    private static com.fasterxml.jackson.databind.JsonNode sampleValue(FieldSpec f) {
        String type = f.getType();
        if ("repeat".equals(type)) {
            ArrayNode arr = NF.arrayNode();
            arr.add(sample(f.getFields()));
            arr.add(sample(f.getFields()));
            return arr;
        }
        if ("u8".equals(type)) {
            return NF.numberNode(1);
        }
        if ("u16".equals(type)) {
            return NF.numberNode(0x1234);
        }
        if ("u32".equals(type)) {
            return NF.numberNode(0x1020304L);
        }
        if ("string".equals(type)) {
            return NF.textNode(f.isVariable() ? "ok" : "abc");
        }
        if ("hex".equals(type)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < f.getLength(); i++) {
                sb.append("ab");
            }
            return NF.textNode(sb.toString());
        }
        if ("time6".equals(type)) {
            ObjectNode t = NF.objectNode();
            t.put("year", 26);
            t.put("month", 1);
            t.put("date", 2);
            t.put("hour", 3);
            t.put("minute", 4);
            t.put("second", 5);
            return t;
        }
        if ("time3".equals(type)) {
            ObjectNode t = NF.objectNode();
            t.put("hour", 3);
            t.put("minute", 4);
            t.put("second", 5);
            return t;
        }
        throw new IllegalArgumentException("unknown field type " + type);
    }
}
