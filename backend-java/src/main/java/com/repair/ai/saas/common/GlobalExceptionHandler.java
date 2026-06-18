package com.repair.ai.saas.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String traceId = genTraceId();
        log.warn("[{}] BusinessException: code={}, message={}", traceId, e.getCode(), e.getMessage());
        HttpStatus httpStatus = mapHttpStatus(e.getCode());
        return ResponseEntity.status(httpStatus).body(ApiResponse.error(e.getCode(), e.getMessage()).withTraceId(traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String traceId = genTraceId();
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("[{}] Validation error: {}", traceId, message);
        return ResponseEntity.badRequest().body(ApiResponse.error(ResultCode.VALIDATION_ERROR, message).withTraceId(traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e, HttpServletRequest request) {
        String traceId = genTraceId();
        log.error("[{}] Unexpected error", traceId, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ResultCode.INTERNAL_ERROR, "服务器内部错误").withTraceId(traceId));
    }

    private String genTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private HttpStatus mapHttpStatus(String code) {
        return switch (code) {
            case ResultCode.NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ResultCode.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ResultCode.FORBIDDEN -> HttpStatus.FORBIDDEN;
            case ResultCode.CONFLICT -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
