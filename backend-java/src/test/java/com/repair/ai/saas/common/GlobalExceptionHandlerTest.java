package com.repair.ai.saas.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理测试。
 * 验证 TOO_MANY_REQUESTS → 429 映射。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ==================== 429 映射 ====================

    @Test
    @DisplayName("TOO_MANY_REQUESTS 应返回 HTTP 429")
    void tooManyRequests_returns429() {
        BusinessException ex = new BusinessException(ResultCode.TOO_MANY_REQUESTS, "请求过于频繁，请稍后重试");
        ResponseEntity<ApiResponse<?>> response = handler.handleBusinessException(ex, null);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ResultCode.TOO_MANY_REQUESTS, response.getBody().getCode());
        assertEquals("请求过于频繁，请稍后重试", response.getBody().getMessage());
    }

    // ==================== 其他状态码映射未受影响 ====================

    @Test
    @DisplayName("NOT_FOUND 应返回 HTTP 404")
    void notFound_returns404() {
        BusinessException ex = new BusinessException(ResultCode.NOT_FOUND, "Not found");
        ResponseEntity<ApiResponse<?>> response = handler.handleBusinessException(ex, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("UNAUTHORIZED 应返回 HTTP 401")
    void unauthorized_returns401() {
        BusinessException ex = new BusinessException(ResultCode.UNAUTHORIZED, "Unauthorized");
        ResponseEntity<ApiResponse<?>> response = handler.handleBusinessException(ex, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("FORBIDDEN 应返回 HTTP 403")
    void forbidden_returns403() {
        BusinessException ex = new BusinessException(ResultCode.FORBIDDEN, "Forbidden");
        ResponseEntity<ApiResponse<?>> response = handler.handleBusinessException(ex, null);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("CONFLICT 应返回 HTTP 409")
    void conflict_returns409() {
        BusinessException ex = new BusinessException(ResultCode.CONFLICT, "Conflict");
        ResponseEntity<ApiResponse<?>> response = handler.handleBusinessException(ex, null);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("BUSINESS_ERROR 应返回 HTTP 400")
    void businessError_returns400() {
        BusinessException ex = new BusinessException(ResultCode.BUSINESS_ERROR, "Business error");
        ResponseEntity<ApiResponse<?>> response = handler.handleBusinessException(ex, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("INVALID_STATE_TRANSITION 应返回 HTTP 400")
    void invalidStateTransition_returns400() {
        BusinessException ex = new BusinessException(ResultCode.INVALID_STATE_TRANSITION, "Invalid state");
        ResponseEntity<ApiResponse<?>> response = handler.handleBusinessException(ex, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
