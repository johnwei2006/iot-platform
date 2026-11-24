package com.iot.auth.internal;

/**
 * Backward-compatible facade delegating to the shared HMAC contract in auth-service-api.
 * Kept so existing imports in this package (and tests) remain unchanged.
 */
public final class HmacSigner {

    public static final String HEADER_SERVICE_ID = com.iot.api.auth.HmacSigner.HEADER_SERVICE_ID;
    public static final String HEADER_TIMESTAMP = com.iot.api.auth.HmacSigner.HEADER_TIMESTAMP;
    public static final String HEADER_SIGNATURE = com.iot.api.auth.HmacSigner.HEADER_SIGNATURE;
    public static final long TIMESTAMP_TOLERANCE_MS = com.iot.api.auth.HmacSigner.TIMESTAMP_TOLERANCE_MS;

    private HmacSigner() {}

    public static String sign(String secret, String serviceId, String timestamp,
                              String method, String path, byte[] body) {
        return com.iot.api.auth.HmacSigner.sign(secret, serviceId, timestamp, method, path, body);
    }

    public static boolean verify(String expected, String actual) {
        return com.iot.api.auth.HmacSigner.verify(expected, actual);
    }

    public static String sha256Hex(byte[] body) {
        return com.iot.api.auth.HmacSigner.sha256Hex(body);
    }
}
