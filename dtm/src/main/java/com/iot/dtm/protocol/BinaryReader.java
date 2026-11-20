package com.iot.dtm.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.dtm.protocol.ProtocolSpec.FieldSpec;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** 单遍顺序读取二进制 serviceData，按 schema 产出结构化 ObjectNode。 */
public class BinaryReader {

    private static final JsonNodeFactory NF = JsonNodeFactory.instance;

    private final byte[] data;
    private final int maxRepeatItems;
    private int pos;

    public BinaryReader(byte[] data, int maxRepeatItems) {
        this.data = data;
        this.maxRepeatItems = maxRepeatItems;
    }

    public ObjectNode read(List<FieldSpec> fields) {
        ObjectNode out = NF.objectNode();
        readFields(fields, out);
        return out;
    }

    public int remaining() {
        return data.length - pos;
    }

    public String remainingHex() {
        return HexCodec.toHex(data, pos, remaining());
    }

    private void readFields(List<FieldSpec> fields, ObjectNode out) {
        for (FieldSpec f : fields) {
            if ("repeat".equals(f.getType())) {
                ArrayNode arr = NF.arrayNode();
                int count = countOf(out, f);
                for (int i = 0; i < count; i++) {
                    ObjectNode child = NF.objectNode();
                    readFields(f.getFields(), child);
                    arr.add(child);
                }
                out.set(f.getName(), arr);
            } else {
                out.set(f.getName(), readScalar(f));
            }
        }
    }

    private int countOf(ObjectNode out, FieldSpec f) {
        JsonNode countNode = out.get(f.getCountRef());
        if (countNode == null || !countNode.isNumber()) {
            throw new CodecException("repeat field " + f.getName()
                    + " references missing/invalid count " + f.getCountRef());
        }
        int count = countNode.asInt();
        if (count < 0) {
            throw new CodecException("negative repeat count " + count + " for " + f.getName());
        }
        if (count > maxRepeatItems) {
            throw new CodecException("repeat count " + count + " exceeds limit " + maxRepeatItems);
        }
        return count;
    }

    private JsonNode readScalar(FieldSpec f) {
        String type = f.getType();
        if ("u8".equals(type)) {
            return NF.numberNode(readU8(f));
        }
        if ("u16".equals(type)) {
            return NF.numberNode(readUnsigned(f, 2));
        }
        if ("u32".equals(type)) {
            return NF.numberNode(readUnsigned(f, 4));
        }
        if ("string".equals(type)) {
            if (f.isVariable()) {
                return NF.textNode(readString(remaining(), f, false));
            }
            return NF.textNode(readString(f.getLength(), f, true));
        }
        if ("hex".equals(type)) {
            return NF.textNode(HexCodec.toHex(readBytes(f.getLength(), f)));
        }
        if ("time6".equals(type)) {
            return readTime(f, 6);
        }
        if ("time3".equals(type)) {
            return readTime(f, 3);
        }
        throw new CodecException("unsupported field type " + type + " for " + f.getName());
    }

    private ObjectNode readTime(FieldSpec f, int size) {
        byte[] b = readBytes(size, f);
        ObjectNode node = NF.objectNode();
        node.put("hex", HexCodec.toHex(b));
        if (size == 6) {
            node.put("year", b[0] & 0xFF);
            node.put("month", b[1] & 0xFF);
            node.put("date", b[2] & 0xFF);
            node.put("hour", b[3] & 0xFF);
            node.put("minute", b[4] & 0xFF);
            node.put("second", b[5] & 0xFF);
        } else {
            node.put("hour", b[0] & 0xFF);
            node.put("minute", b[1] & 0xFF);
            node.put("second", b[2] & 0xFF);
        }
        return node;
    }

    private long readUnsigned(FieldSpec f, int size) {
        byte[] b = readBytes(size, f);
        long value = 0;
        if (f.isLittleEndian()) {
            for (int i = size - 1; i >= 0; i--) {
                value = (value << 8) | (b[i] & 0xFF);
            }
        } else {
            for (int i = 0; i < size; i++) {
                value = (value << 8) | (b[i] & 0xFF);
            }
        }
        return value;
    }

    private int readU8(FieldSpec f) {
        return readBytes(1, f)[0] & 0xFF;
    }

    private String readString(int length, FieldSpec f, boolean trimPadding) {
        byte[] b = readBytes(length, f);
        String text = new String(b, StandardCharsets.UTF_8);
        return trimPadding ? text.replaceAll("\u0000+$", "") : text;
    }

    private byte[] readBytes(int length, FieldSpec f) {
        if (length < 0 || pos + length > data.length) {
            throw new CodecException("truncated field " + f.getName() + " at offset " + pos
                    + ": need " + length + " bytes, available " + (data.length - pos));
        }
        byte[] out = new byte[length];
        System.arraycopy(data, pos, out, 0, length);
        pos += length;
        return out;
    }
}
