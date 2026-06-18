package com.repair.ai.saas.security;

import com.repair.ai.saas.module.user.entity.SysUser;
import com.repair.ai.saas.module.user.service.SysUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/public/**"
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserService sysUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // 白名单放行
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取 JWT
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            writeUnauthorized(response, "Token无效或已过期");
            return;
        }

        var claims = jwtTokenProvider.parseToken(token);
        var tokenInfo = JwtTokenProvider.TokenInfo.fromClaims(claims);

        // 实时校验：用户必须存在、未被禁用、未被删除、租户匹配
        SysUser dbUser = sysUserService.getActiveUserForAuth(tokenInfo.userId, tokenInfo.tenantId);
        if (dbUser == null) {
            writeUnauthorized(response, "账号不存在或已被禁用");
            return;
        }

        UserContext.set(new CurrentUser(
                dbUser.getId(),
                dbUser.getTenantId(),
                dbUser.getUsername(),
                dbUser.getRole()
        ));

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(p -> PATH_MATCHER.match(p, path));
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"" + message + "\",\"data\":null,\"traceId\":null}");
    }
}
