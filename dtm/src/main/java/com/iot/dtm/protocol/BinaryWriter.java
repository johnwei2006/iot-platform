package com.iot.dtm.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.dtm.protocol.ProtocolSpec.FieldSpec;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 与 BinaryReader 对称的顺序写入器：结构化字段重新编码为设备可识别的二进制。 */
public class BinaryWriter {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream(256);

    public void write(List<FieldSpec> fields, JsonNode node) {
        writeFields(fields, node == null ? null : node, collectCounts(fields, node));
    }

    public byte[] toByteArray() {
        return out.toByteArray();
    }

    private void writeFields(List<FieldSpec> fields, JsonNode node, Map<String, Integer> counts) {
        for (FieldSpec f : fields) {
            if ("repeat".equals(f.getType())) {
                JsonNode arr = node == null ? null : node.get(f.getName());
                if (arr != null && arr.isArray()) {
                    for (JsonNode child : arr) {
                        writeFields(f.getFields(), child, collectCounts(f.getFields(), child));
                    }
                }
            } else {
                writeScalar(f, node, counts);
            }
        }
    }

    /**
     * 计数字段以实际数组长度为准，避免后台传来的结构化对象里 count 与数组不一致。
     * 嵌套 repeat 的计数在写入每个子元素时重新计算。
     */
    private Map<String, Integer> collectCounts(List<FieldSpec> fields, JsonNode node) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (FieldSpec f : fields) {
            if ("repeat".equals(f.getType()) && f.getCountRef() != null) {
                JsonNode arr = node == null ? null : node.get(f.getName());
                counts.put(f.getCountRef(), arr != null && arr.isArray() ? arr.size() : 0);
            }
        }
        return counts;
    }

    private void writeScalar(FieldSpec f, JsonNode node, Map<String, Integer> counts) {
        String type = f.getType();
        JsonNode value = node == null ? null : node.get(f.getName());
        if ("u8".equals(type)) {
            out.write((int) (number(f, value, counts) & 0xFF));
            return;
        }
        if ("u16".equals(type) || "u32".equals(type)) {
            int size = "u16".equals(type) ? 2 : 4;
            long v = number(f, value, counts);
            for (int i = 0; i < size; i++) {
                int shift = (f.isLittleEndian() ? i : size - 1 - i) * 8;
                out.write((int) ((v >>> shift) & 0xFF));
            }
            return;
        }
        if ("string".equals(type)) {
            byte[] bytes = value == null || value.isNull() ? new byte[0]
                    : value.asText().getBytes(StandardCharsets.UTF_8);
            if (f.isVariable()) {
                out.write(bytes, 0, bytes.length);
            } else {
                writePadded(bytes, f.getLength(), f);
            }
            return;
        }
        if ("hex".equals(type)) {
            byte[] bytes = value == null || value.isNull() ? new byte[0] : HexCodec.fromHex(value.asText());
            writePadded(bytes, f.getLength(), f);
            return;
        }
        if ("time6".equals(type) || "time3".equals(type)) {
            writePadded(timeBytes(value, "time6".equals(type) ? 6 : 3),
                    "time6".equals(type) ? 6 : 3, f);
            return;
        }
        throw new CodecException("unsupported field type " + type + " for " + f.getName());
    }

    private long number(FieldSpec f, JsonNode value, Map<String, Integer> counts) {
        Integer override = counts.get(f.getName());
        if (override != null) {
            return override.longValue();
        }
        if (value == null || value.isNull()) {
            return 0;
        }
        if (!value.isNumber() && !value.isTextual()) {
            throw new CodecException("field " + f.getName() + " expects a number, got " + value.getNodeType());
        }
        return value.asLong();
    }

    private byte[] timeBytes(JsonNode value, int size) {
        byte[] b = new byte[size];
        if (value == null || value.isNull()) {
            return b;
        }
        if (value.isTextual()) {
            byte[] parsed = HexCodec.fromHex(value.asText());
            System.arraycopy(parsed, 0, b, 0, Math.min(parsed.length, size));
            return b;
        }
        if (!value.isObject()) {
            throw new CodecException("time field expects object or hex string, got " + value.getNodeType());
        }
        if (size == 6) {
            b[0] = (byte) value.path("year").asInt();
            b[1] = (byte) value.path("month").asInt();
            b[2] = (byte) value.path("date").asInt();
            b[3] = (byte) value.path("hour").asInt();
            b[4] = (byte) value.path("minute").asInt();
            b[5] = (byte) value.path("second").asInt();
        } else {
            b[0] = (byte) value.path("hour").asInt();
            b[1] = (byte) value.path("minute").asInt();
            b[2] = (byte) value.path("second").asInt();
        }
        return b;
    }

    private void writePadded(byte[] bytes, int length, FieldSpec f) {
        if (bytes.length > length) {
            throw new CodecException("field " + f.getName() + " is " + bytes.length
                    + " bytes, exceeds declared length " + length);
        }
        out.write(bytes, 0, bytes.length);
        for (int i = bytes.length; i < length; i++) {
            out.write(0);
        }
    }
}
