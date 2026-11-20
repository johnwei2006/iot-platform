package com.iot.auth.common;

/** 类型化业务异常，带业务码与 HTTP 状态码。 */
public class AppException extends RuntimeException {

    private final ResultCode resultCode;

    public AppException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public AppException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    public static AppException badRequest(String message) {
        return new AppException(ResultCode.BAD_REQUEST, message);
    }

    public static AppException unauthorized(String message) {
        return new AppException(ResultCode.UNAUTHORIZED, message);
    }

    public static AppException forbidden(String message) {
        return new AppException(ResultCode.FORBIDDEN, message);
    }

    public static AppException notFound(String message) {
        return new AppException(ResultCode.NOT_FOUND, message);
    }

    public static AppException conflict(String message) {
        return new AppException(ResultCode.CONFLICT, message);
    }
}
