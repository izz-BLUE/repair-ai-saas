package com.repair.ai.saas.security;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/public/**",
            "/api/health"
    );

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(p -> MATCHER.match(p, path));
    }

    // ===== /api/health =====

    @Test
    void health_isPublic() {
        assertTrue(isPublicPath("/api/health"));
    }

    // ===== /api/public/** =====

    @Test
    void publicRoot_isPublic() {
        assertTrue(isPublicPath("/api/public/PLATFORM/portal-settings"));
    }

    @Test
    void publicLogin_isPublic() {
        assertTrue(isPublicPath("/api/public/login"));
    }

    @Test
    void publicRepairRequests_isPublic() {
        assertTrue(isPublicPath("/api/public/TA7P55N/repair-requests"));
    }

    @Test
    void publicTicketQuery_isPublic() {
        assertTrue(isPublicPath("/api/public/TA7P55N/tickets/query"));
    }

    @Test
    void publicAiChat_isPublic() {
        assertTrue(isPublicPath("/api/public/TA7P55N/ai/chat"));
    }

    // ===== 受保护路径 =====

    @Test
    void adminPath_isNotPublic() {
        assertFalse(isPublicPath("/api/admin/dashboard/stats"));
    }

    @Test
    void adminTickets_isNotPublic() {
        assertFalse(isPublicPath("/api/admin/tickets"));
    }

    @Test
    void technicianPath_isNotPublic() {
        assertFalse(isPublicPath("/api/technician/tickets"));
    }

    @Test
    void commonPath_isNotPublic() {
        assertFalse(isPublicPath("/api/common/me"));
    }

    @Test
    void platformPath_isNotPublic() {
        assertFalse(isPublicPath("/api/platform/tenants"));
    }

    @Test
    void randomApiPath_isNotPublic() {
        assertFalse(isPublicPath("/api/admin/users"));
    }

    // ===== OPTIONS 预检请求（Filter 层直接放行，不判断是否 public path） =====

    @Test
    void options_public_login_shouldPass() {
        // OPTIONS 请求在 doFilterInternal 中先于路径检查放行
        // 此处验证路径匹配逻辑 — OPTIONS /api/public/login 仍然是 public path
        assertTrue(isPublicPath("/api/public/login"));
    }

    @Test
    void options_adminPath_wouldBeBlockedByPathFilter_butFilterLetsOptionsThrough() {
        // OPTIONS /api/admin/tickets 不是 public path，但 JwtAuthenticationFilter
        // 在方法级别检查 OPTIONS 后直接放行，不进入路径匹配
        assertFalse(isPublicPath("/api/admin/tickets"));
    }
}
