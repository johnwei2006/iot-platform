package com.iot.api.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Shared HMAC-SHA256 signer for internal service calls. Contract identical on both sides. */
public final class HmacSigner {

    public static final String HEADER_SERVICE_ID = "X-Service-Id";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final long TIMESTAMP_TOLERANCE_MS = 5L * 60 * 1000;

    private HmacSigner() {}

    public static String sign(String secret, String serviceId, String timestamp,
                              String method, String path, byte[] body) {
        String content = serviceId + "\n" + timestamp + "\n" + method + "\n" + path + "\n" + sha256Hex(body);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return hex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    public static boolean verify(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] body) {
        byte[] data = body == null ? new byte[0] : body;
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
