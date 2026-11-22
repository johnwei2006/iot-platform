package com.iot.auth.internal;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 内部服务 HMAC-SHA256 签名（auth-service 与 admin-service 共同约定）：
 *
 * <pre>
 * X-Service-Id: 服务标识（须在 auth.service-keys 中注册）
 * X-Timestamp:  毫秒时间戳（±5 分钟有效）
 * X-Signature:  HMAC-SHA256(secret, serviceId + "\n" + timestamp + "\n"
 *                            + method + "\n" + path + "\n" + sha256hex(body))
 * </pre>
 * body 为原始字节（GET 为空串）。比对使用常量时间，防时序攻击。
 */
public final class HmacSigner {

    public static final String HEADER_SERVICE_ID = "X-Service-Id";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Signature";
    /** 时间戳允许偏移（毫秒） */
    public static final long TIMESTAMP_TOLERANCE_MS = 5L * 60 * 1000;

    private HmacSigner() {
    }

    public static String sign(String secret, String serviceId, String timestamp,
                              String method, String path, byte[] body) {
        String content = serviceId + "\n" + timestamp + "\n" + method + "\n" + path
                + "\n" + sha256Hex(body);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    /** 常量时间签名比对。 */
    public static boolean verify(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] body) {
        byte[] data = body == null ? new byte[0] : body;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest(data)) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
