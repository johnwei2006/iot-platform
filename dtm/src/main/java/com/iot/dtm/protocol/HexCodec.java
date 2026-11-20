package com.iot.dtm.protocol;

/** 字节数组与十六进制字符串互转，用于 hex 字段与残留字节的可观测输出。 */
public final class HexCodec {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private HexCodec() {
    }

    public static String toHex(byte[] bytes) {
        return toHex(bytes, 0, bytes.length);
    }

    public static String toHex(byte[] bytes, int offset, int length) {
        char[] out = new char[length * 2];
        for (int i = 0; i < length; i++) {
            int v = bytes[offset + i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    public static byte[] fromHex(String text) {
        if (text == null || text.length() == 0) {
            return new byte[0];
        }
        String s = text.startsWith("0x") || text.startsWith("0X") ? text.substring(2) : text;
        if (s.length() % 2 != 0) {
            throw new CodecException("hex string has odd length: " + s.length());
        }
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(s.charAt(i * 2), 16);
            int lo = Character.digit(s.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new CodecException("invalid hex character in: " + s);
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
