package com.iot.auth.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局错误处理：业务异常按约定格式返回，未知异常只记录日志不暴露堆栈。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException e) {
        return ResponseEntity.status(httpStatus(e.getResultCode()))
                .body(ApiResponse.fail(e.getResultCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String field = e.getBindingResult().getFieldErrors().isEmpty()
                ? "request" : e.getBindingResult().getFieldErrors().get(0).getField();
        return ResponseEntity.badRequest().body(ApiResponse.fail(400, "invalid field: " + field));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("unexpected auth-service error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ResultCode.ERROR));
    }

    private HttpStatus httpStatus(ResultCode rc) {
        switch (rc) {
            case BAD_REQUEST: return HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED: return HttpStatus.UNAUTHORIZED;
            case FORBIDDEN: return HttpStatus.FORBIDDEN;
            case NOT_FOUND: return HttpStatus.NOT_FOUND;
            case CONFLICT: return HttpStatus.CONFLICT;
            case RATE_LIMITED: return HttpStatus.TOO_MANY_REQUESTS;
            default: return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
