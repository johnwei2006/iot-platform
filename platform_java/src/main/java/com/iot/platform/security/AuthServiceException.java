package com.iot.platform.security;

/**
 * auth-service 调用失败。区分「令牌无效」(401) 与「认证服务不可用」(503)，
 * 便于过滤器返回准确的状态码，同时保持 fail-closed（不放行）语义。
 */
public class AuthServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    private AuthServiceException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static AuthServiceException unauthorized(String message) {
        return new AuthServiceException(401, message);
    }

    public static AuthServiceException unavailable(String message) {
        return new AuthServiceException(503, message);
    }

    public int getStatus() {
        return status;
    }
}
