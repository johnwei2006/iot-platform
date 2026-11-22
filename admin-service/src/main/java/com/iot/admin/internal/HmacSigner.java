package com.iot.admin.internal;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 内部服务 HMAC-SHA256 签名（与 auth-service internal/HmacSigner 共同约定，算法一致）：
 * X-Signature = HMAC-SHA256(secret, serviceId + "\n" + timestamp + "\n"
 *                            + method + "\n" + path + "\n" + sha256hex(body))
 */
public final class HmacSigner {

    public static final String HEADER_SERVICE_ID = "X-Service-Id";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Signature";

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
