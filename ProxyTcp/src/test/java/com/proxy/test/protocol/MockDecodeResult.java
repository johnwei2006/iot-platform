package com.proxy.test.protocol;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 模拟设备侧的业务解码结果。
 *
 * <p>与 dtm 的 DecodeResult 语义保持一致：
 * OK/EMPTY 为成功，UNSUPPORTED 表示协议无 schema、加密或编码不支持，
 * FAILED 表示报文截断、字段缺失等真实解析错误。
 */
public final class MockDecodeResult {

    public enum Status {
        OK, EMPTY, UNSUPPORTED, FAILED
    }

    private final Status status;
    private final JsonNode business;
    private final String error;
    private final byte[] raw;

    private MockDecodeResult(Status status, JsonNode business, String error, byte[] raw) {
        this.status = status;
        this.business = business;
        this.error = error;
        this.raw = raw;
    }

    public static MockDecodeResult ok(JsonNode business) {
        return new MockDecodeResult(Status.OK, business, null, null);
    }

    public static MockDecodeResult empty(JsonNode business) {
        return new MockDecodeResult(Status.EMPTY, business, null, null);
    }

    public static MockDecodeResult unsupported(String error, byte[] raw) {
        return new MockDecodeResult(Status.UNSUPPORTED, null, error, raw);
    }

    public static MockDecodeResult failed(String error, byte[] raw) {
        return new MockDecodeResult(Status.FAILED, null, error, raw);
    }

    public Status getStatus() {
        return status;
    }

    public JsonNode getBusiness() {
        return business;
    }

    public String getError() {
        return error;
    }

    public byte[] getRaw() {
        return raw;
    }

    public boolean isSuccess() {
        return status == Status.OK || status == Status.EMPTY;
    }

    @Override
    public String toString() {
        if (error != null) {
            return status + "(" + error + ")";
        }
        return status.toString();
    }
}
