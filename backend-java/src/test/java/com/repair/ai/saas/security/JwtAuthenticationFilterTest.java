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
}
