package com.aimedical.config;

import com.aimedical.modules.commonmodule.api.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT认证过滤器
 *
 * <p>验证JWT令牌并设置Spring Security上下文。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(authHeader);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!JwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从令牌中获取用户信息
        Long userId = JwtUtil.getUserId(token);
        String username = JwtUtil.getUsername(token);
        String role = JwtUtil.getRole(token);

        // 创建认证对象
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                java.util.List.of(authority)
        );

        // 设置认证详情
        authentication.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource()
                .buildDetails(request));

        // 设置Security上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    /**
     * 从Authorization头中提取JWT令牌
     *
     * @param authHeader Authorization请求头
     * @return JWT令牌，如果无效则返回null
     */
    private String extractToken(String authHeader) {
        String tokenType = JwtUtil.getTokenType();
        if (authHeader.startsWith(tokenType + " ")) {
            return authHeader.substring(tokenType.length() + 1);
        }
        return null;
    }
}