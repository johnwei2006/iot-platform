package com.proxy.test.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * ProxyTcp 测试侧自包含业务编解码器。
 *
 * <p>不依赖 dtm 模块，直接读取 test resources 下的 protocol-schemas.json，
 * 按字段顺序编解码 u8/u16/u32/string/hex/time6/time3/repeat，供 DeviceMockTCP
 * 生成和解析 37 类平台-网关协议报文。
 */
public final class MockProtocolCodec {

    public static final int SERVICE_ACK = 0;
    public static final int ENCODING_JSON = 1;
    public static final int ENCODING_BINARY = 2;
    public static final int PROTOCOL_VERSION_1 = 1;
    public static final int ENCRYPTION_NONE = 0;
    public static final int MAX_REPEAT_ITEMS = 256;
    public static final int MAX_PAYLOAD_BYTES = 8192;

    private static final String RESOURCE = "protocol-schemas.json";
    private static final JsonNodeFactory NF = JsonNodeFactory.instance;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<Integer, JsonNode> protocols = new LinkedHashMap<Integer, JsonNode>();

    public MockProtocolCodec() {
        load();
    }

    private void load() {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE);
        if (in == null) {
            in = MockProtocolCodec.class.getResourceAsStream("/" + RESOURCE);
        }
        if (in == null) {
            throw new IllegalStateException("classpath resource not found: " + RESOURCE);
        }
        try {
            JsonNode root = MAPPER.readTree(in);
            JsonNode list = root == null ? null : root.get("protocols");
            if (list == null || !list.isArray()) {
                throw new IllegalStateException("invalid protocol schema: missing protocols array");
            }
            for (JsonNode protocol : list) {
                String hex = protocol.path("hex").asText();
                if (hex.length() == 0) {
                    throw new IllegalStateException("protocol without hex in " + RESOURCE);
                }
                int code = Integer.decode(hex);
                if (protocols.put(code, protocol) != null) {
                    throw new IllegalStateException("duplicate serviceType " + hex);
                }
            }
            if (protocols.isEmpty()) {
                throw new IllegalStateException("empty protocol schema: " + RESOURCE);
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to load " + RESOURCE, e);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    public Collection<Integer> serviceTypes() {
        return protocols.keySet();
    }

    public int size() {
        return protocols.size();
    }

    public JsonNode protocol(int serviceType) {
        return protocols.get(serviceType);
    }

    public String name(int serviceType) {
        if (serviceType == SERVICE_ACK) {
            return "SERVICE_ACK";
        }
        JsonNode p = protocols.get(serviceType);
        return p == null ? "UNKNOWN" : p.path("name").asText("UNKNOWN");
    }

    public String direction(int serviceType) {
        if (serviceType == SERVICE_ACK) {
            return "downlink";
        }
        JsonNode p = protocols.get(serviceType);
        return p == null ? "unknown" : p.path("direction").asText("unknown");
    }

    public String hex(int serviceType) {
        return String.format("0x%02X", serviceType);
    }

    public List<MockServiceCase> allCases() {
        List<MockServiceCase> cases = new ArrayList<MockServiceCase>();
        for (Map.Entry<Integer, JsonNode> entry : protocols.entrySet()) {
            JsonNode p = entry.getValue();
            cases.add(new MockServiceCase(entry.getKey(), p.path("name").asText(),
                    p.path("direction").asText(), sample(p), p.path("unsupported").asBoolean()));
        }
        return cases;
    }

    public JsonNode sample(int serviceType) {
        JsonNode p = protocols.get(serviceType);
        if (p == null) {
            throw new IllegalArgumentException("unknown serviceType: " + serviceType);
        }
        return sample(p);
    }

    private JsonNode sample(JsonNode protocol) {
        ObjectNode out = NF.objectNode();
        if (protocol.path("unsupported").asBoolean()) {
            return out;
        }
        writeSampleFields(protocol.path("fields"), out);
        return out;
    }

    private void writeSampleFields(JsonNode fields, ObjectNode out) {
        if (fields == null || !fields.isArray()) {
            return;
        }
        for (JsonNode field : fields) {
            String type = field.path("type").asText();
            String name = field.path("name").asText();
            if ("repeat".equals(type)) {
                String countRef = field.path("countRef").asText();
                if (countRef.length() > 0) {
                    out.put(countRef, 1);
                }
                ArrayNode array = out.putArray(name);
                ObjectNode child = NF.objectNode();
                writeSampleFields(field.path("fields"), child);
                array.add(child);
            } else {
                out.set(name, sampleScalar(field));
            }
        }
    }

    private JsonNode sampleScalar(JsonNode field) {
        String type = field.path("type").asText();
        if ("u8".equals(type) || "u16".equals(type) || "u32".equals(type)) {
            return NF.numberNode(1);
        }
        if ("string".equals(type)) {
            if (field.path("variable").asBoolean()) {
                return NF.textNode("MOCK");
            }
            return NF.textNode(repeat("A", field.path("length").asInt()));
        }
        if ("hex".equals(type)) {
            return NF.textNode(repeat("01", field.path("length").asInt()));
        }
        if ("time6".equals(type)) {
            ObjectNode time = NF.objectNode();
            time.put("year", 26);
            time.put("month", 11);
            time.put("date", 10);
            time.put("hour", 12);
            time.put("minute", 30);
            time.put("second", 45);
            return time;
        }
        if ("time3".equals(type)) {
            ObjectNode time = NF.objectNode();
            time.put("hour", 12);
            time.put("minute", 30);
            time.put("second", 45);
            return time;
        }
        throw new IllegalStateException("unsupported field type " + type
                + " for " + field.path("name").asText());
    }

    private static String repeat(String text, int count) {
        StringBuilder sb = new StringBuilder(text.length() * Math.max(count, 0));
        for (int i = 0; i < count; i++) {
            sb.append(text);
        }
        return sb.toString();
    }

    /**
     * 按 schema 将结构化业务字段编码为二进制 serviceData。
     */
    public byte[] encode(int serviceType, JsonNode decoded) {
        JsonNode protocol = protocols.get(serviceType);
        if (protocol == null) {
            throw new IllegalArgumentException("unknown serviceType: " + serviceType);
        }
        if (protocol.path("unsupported").asBoolean()) {
            throw new IllegalArgumentException("serviceType " + hex(serviceType)
                    + " has no field schema: " + protocol.path("name").asText());
        }
        JsonNode fields = protocol.path("fields");
        if (fields == null || fields.size() == 0) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(256);
        writeFields(fields, decoded == null ? NF.objectNode() : decoded, out);
        byte[] bytes = out.toByteArray();
        if (bytes.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("encoded serviceData length " + bytes.length
                    + " exceeds limit " + MAX_PAYLOAD_BYTES);
        }
        return bytes;
    }

    private void writeFields(JsonNode fields, JsonNode node, ByteArrayOutputStream out) {
        if (fields == null || !fields.isArray()) {
            return;
        }
        for (JsonNode field : fields) {
            String type = field.path("type").asText();
            if ("repeat".equals(type)) {
                JsonNode array = node == null ? null : node.get(field.path("name").asText());
                if (array != null && array.isArray()) {
                    for (JsonNode child : array) {
                        writeFields(field.path("fields"), child, out);
                    }
                }
            } else {
                writeScalar(field, node, out);
            }
        }
    }

    private void writeScalar(JsonNode field, JsonNode node, ByteArrayOutputStream out) {
        String type = field.path("type").asText();
        String name = field.path("name").asText();
        JsonNode value = node == null ? null : node.get(name);
        if ("u8".equals(type)) {
            out.write((int) (number(field, value) & 0xFF));
            return;
        }
        if ("u16".equals(type) || "u32".equals(type)) {
            int size = "u16".equals(type) ? 2 : 4;
            long v = number(field, value);
            boolean little = "little".equalsIgnoreCase(field.path("endian").asText());
            for (int i = 0; i < size; i++) {
                int shift = (little ? i : size - 1 - i) * 8;
                out.write((int) ((v >>> shift) & 0xFF));
            }
            return;
        }
        if ("string".equals(type)) {
            byte[] bytes = value == null || value.isNull() ? new byte[0]
                    : value.asText().getBytes(StandardCharsets.UTF_8);
            if (field.path("variable").asBoolean()) {
                out.write(bytes, 0, bytes.length);
            } else {
                writePadded(bytes, field.path("length").asInt(), name, out);
            }
            return;
        }
        if ("hex".equals(type)) {
            byte[] bytes = value == null || value.isNull() ? new byte[0] : fromHex(value.asText());
            writePadded(bytes, field.path("length").asInt(), name, out);
            return;
        }
        if ("time6".equals(type) || "time3".equals(type)) {
            int size = "time6".equals(type) ? 6 : 3;
            writePadded(timeBytes(value, size), size, name, out);
            return;
        }
        throw new IllegalArgumentException("unsupported field type " + type + " for " + name);
    }

    private long number(JsonNode field, JsonNode value) {
        if (value == null || value.isNull()) {
            return 0;
        }
        if (!value.isNumber() && !value.isTextual()) {
            throw new IllegalArgumentException("field " + field.path("name").asText()
                    + " expects a number, got " + value.getNodeType());
        }
        return value.asLong();
    }

    private byte[] timeBytes(JsonNode value, int size) {
        byte[] bytes = new byte[size];
        if (value == null || value.isNull()) {
            return bytes;
        }
        if (value.isTextual()) {
            byte[] parsed = fromHex(value.asText());
            System.arraycopy(parsed, 0, bytes, 0, Math.min(parsed.length, size));
            return bytes;
        }
        if (!value.isObject()) {
            throw new IllegalArgumentException("time field expects object or hex string");
        }
        if (size == 6) {
            bytes[0] = (byte) value.path("year").asInt();
            bytes[1] = (byte) value.path("month").asInt();
            bytes[2] = (byte) value.path("date").asInt();
            bytes[3] = (byte) value.path("hour").asInt();
            bytes[4] = (byte) value.path("minute").asInt();
            bytes[5] = (byte) value.path("second").asInt();
        } else {
            bytes[0] = (byte) value.path("hour").asInt();
            bytes[1] = (byte) value.path("minute").asInt();
            bytes[2] = (byte) value.path("second").asInt();
        }
        return bytes;
    }

    private void writePadded(byte[] bytes, int length, String fieldName, ByteArrayOutputStream out) {
        if (length < 0) {
            throw new IllegalArgumentException("negative length for field " + fieldName);
        }
        if (bytes.length > length) {
            throw new IllegalArgumentException("field " + fieldName + " is " + bytes.length
                    + " bytes, exceeds declared length " + length);
        }
        out.write(bytes, 0, bytes.length);
        for (int i = bytes.length; i < length; i++) {
            out.write(0);
        }
    }

    /**
     * 解码设备收到的 serviceData。编码只支持 1(JSON) / 2(BINARY)，加密只支持 0。
     */
    public MockDecodeResult decode(int serviceType, int encoding, int protocolVersion,
                                   int encryption, byte[] serviceData) {
        byte[] raw = serviceData == null ? new byte[0] : serviceData;
        if (encryption != ENCRYPTION_NONE) {
            return MockDecodeResult.unsupported("encrypted serviceData is not supported: "
                    + encryption, raw);
        }
        if (serviceType == SERVICE_ACK) {
            if (raw.length == 0) {
                return MockDecodeResult.empty(NF.objectNode());
            }
            return MockDecodeResult.unsupported("SERVICE_ACK payload is not empty: "
                    + raw.length + " bytes", raw);
        }
        if (encoding == ENCODING_JSON) {
            if (raw.length == 0) {
                return MockDecodeResult.failed("empty JSON serviceData", raw);
            }
            try {
                JsonNode parsed = MAPPER.readTree(new String(raw, StandardCharsets.UTF_8));
                if (parsed == null || !parsed.isObject()) {
                    return MockDecodeResult.failed("JSON serviceData is not an object", raw);
                }
                return MockDecodeResult.ok(parsed);
            } catch (Exception e) {
                return MockDecodeResult.failed("invalid JSON serviceData: " + e.getMessage(), raw);
            }
        }
        if (encoding != ENCODING_BINARY) {
            return MockDecodeResult.unsupported("unsupported serviceDataEncoding="
                    + encoding, raw);
        }
        if (protocolVersion != PROTOCOL_VERSION_1) {
            return MockDecodeResult.unsupported("unsupported serviceDataProtocolVersion="
                    + protocolVersion, raw);
        }
        JsonNode protocol = protocols.get(serviceType);
        if (protocol == null) {
            return MockDecodeResult.unsupported("unknown serviceType " + serviceType, raw);
        }
        if (protocol.path("unsupported").asBoolean()) {
            return MockDecodeResult.unsupported("protocol " + protocol.path("hex").asText()
                    + " has no field schema", raw);
        }
        JsonNode fields = protocol.path("fields");
        if (fields == null || fields.size() == 0) {
            return raw.length == 0
                    ? MockDecodeResult.empty(NF.objectNode())
                    : MockDecodeResult.failed("expected empty serviceData but got "
                            + raw.length + " bytes", raw);
        }
        try {
            Cursor cursor = new Cursor(raw);
            ObjectNode decoded = NF.objectNode();
            readFields(fields, cursor, decoded);
            if (cursor.remaining() > 0) {
                decoded.put("unparsedBytes", toHex(raw, cursor.pos, cursor.remaining()));
            }
            return MockDecodeResult.ok(decoded);
        } catch (Exception e) {
            return MockDecodeResult.failed(e.getMessage() == null
                    ? e.getClass().getSimpleName() : e.getMessage(), raw);
        }
    }

    private void readFields(JsonNode fields, Cursor cursor, ObjectNode out) {
        if (fields == null || !fields.isArray()) {
            return;
        }
        for (JsonNode field : fields) {
            String type = field.path("type").asText();
            String name = field.path("name").asText();
            if ("repeat".equals(type)) {
                String countRef = field.path("countRef").asText();
                JsonNode countNode = out.get(countRef);
                if (countNode == null || !countNode.isNumber()) {
                    throw new IllegalArgumentException("repeat field " + name
                            + " references missing/invalid count " + countRef);
                }
                int count = countNode.asInt();
                if (count < 0 || count > MAX_REPEAT_ITEMS) {
                    throw new IllegalArgumentException("repeat count " + count
                            + " out of range for " + name);
                }
                ArrayNode array = out.putArray(name);
                for (int i = 0; i < count; i++) {
                    ObjectNode child = NF.objectNode();
                    readFields(field.path("fields"), cursor, child);
                    array.add(child);
                }
            } else {
                out.set(name, readScalar(field, cursor));
            }
        }
    }

    private JsonNode readScalar(JsonNode field, Cursor cursor) {
        String type = field.path("type").asText();
        String name = field.path("name").asText();
        if ("u8".equals(type)) {
            return NF.numberNode(cursor.readUnsigned(1, false) & 0xFF);
        }
        if ("u16".equals(type)) {
            boolean little = "little".equalsIgnoreCase(field.path("endian").asText());
            return NF.numberNode(cursor.readUnsigned(2, little));
        }
        if ("u32".equals(type)) {
            boolean little = "little".equalsIgnoreCase(field.path("endian").asText());
            return NF.numberNode(cursor.readUnsigned(4, little));
        }
        if ("string".equals(type)) {
            int length = field.path("variable").asBoolean()
                    ? cursor.remaining() : field.path("length").asInt();
            return NF.textNode(trimPadding(new String(cursor.readBytes(length, name),
                    StandardCharsets.UTF_8)));
        }
        if ("hex".equals(type)) {
            byte[] bytes = cursor.readBytes(field.path("length").asInt(), name);
            return NF.textNode(toHex(bytes, 0, bytes.length));
        }
        if ("time6".equals(type)) {
            return timeNode(cursor.readBytes(6, name));
        }
        if ("time3".equals(type)) {
            return timeNode(cursor.readBytes(3, name));
        }
        throw new IllegalArgumentException("unsupported field type " + type + " for " + name);
    }

    private JsonNode timeNode(byte[] bytes) {
        ObjectNode node = NF.objectNode();
        node.put("hex", toHex(bytes, 0, bytes.length));
        if (bytes.length == 6) {
            node.put("year", bytes[0] & 0xFF);
            node.put("month", bytes[1] & 0xFF);
            node.put("date", bytes[2] & 0xFF);
            node.put("hour", bytes[3] & 0xFF);
            node.put("minute", bytes[4] & 0xFF);
            node.put("second", bytes[5] & 0xFF);
        } else {
            node.put("hour", bytes[0] & 0xFF);
            node.put("minute", bytes[1] & 0xFF);
            node.put("second", bytes[2] & 0xFF);
        }
        return node;
    }

    private static String trimPadding(String text) {
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '\0') {
            end--;
        }
        return text.substring(0, end);
    }

    private static String toHex(byte[] bytes, int offset, int length) {
        char[] hex = "0123456789abcdef".toCharArray();
        char[] out = new char[length * 2];
        for (int i = 0; i < length; i++) {
            int v = bytes[offset + i] & 0xFF;
            out[i * 2] = hex[v >>> 4];
            out[i * 2 + 1] = hex[v & 0x0F];
        }
        return new String(out);
    }

    private static byte[] fromHex(String text) {
        if (text == null || text.length() == 0) {
            return new byte[0];
        }
        String s = text.startsWith("0x") || text.startsWith("0X") ? text.substring(2) : text;
        if (s.length() % 2 != 0) {
            throw new IllegalArgumentException("hex string has odd length: " + s.length());
        }
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(s.charAt(i * 2), 16);
            int lo = Character.digit(s.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("invalid hex character in: " + s);
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static final class Cursor {
        private final byte[] data;
        private int pos;

        private Cursor(byte[] data) {
            this.data = data == null ? new byte[0] : data;
        }

        private int remaining() {
            return data.length - pos;
        }

        private long readUnsigned(int size, boolean little) {
            byte[] bytes = readBytes(size, "number");
            long value = 0;
            if (little) {
                for (int i = size - 1; i >= 0; i--) {
                    value = (value << 8) | (bytes[i] & 0xFF);
                }
            } else {
                for (int i = 0; i < size; i++) {
                    value = (value << 8) | (bytes[i] & 0xFF);
                }
            }
            return value;
        }

        private byte[] readBytes(int length, String fieldName) {
            if (length < 0 || pos + length > data.length) {
                throw new IllegalArgumentException("truncated field " + fieldName
                        + " at offset " + pos + ": need " + length + " bytes, available "
                        + (data.length - pos));
            }
            byte[] out = new byte[length];
            System.arraycopy(data, pos, out, 0, length);
            pos += length;
            return out;
        }
    }
}
