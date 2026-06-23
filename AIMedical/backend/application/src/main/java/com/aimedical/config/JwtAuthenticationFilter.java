package com.aimedical.config;

import com.aimedical.modules.commonmodule.jwt.JwtUtil;

import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT认证过滤器
 *
 * <p>验证JWT令牌并设置Spring Security上下文。
 * 使用统一的Claims对象避免重复解析JWT。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

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

        // 统一验证并获取Claims，避免重复解析JWT
        Claims claims = jwtUtil.validateTokenAndGetClaims(token);
        if (claims == null) {
            log.debug("JWT令牌验证失败");
            filterChain.doFilter(request, response);
            return;
        }

        // 从Claims中提取用户信息
        Long userId = extractUserId(claims);
        if (userId == null) {
            log.debug("从JWT令牌中获取用户ID失败");
            filterChain.doFilter(request, response);
            return;
        }

        String role = extractRole(claims);
        if (role == null) {
            log.debug("从JWT令牌中获取角色失败");
            filterChain.doFilter(request, response);
            return;
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(authority)
        );

        authentication.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource()
                .buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String extractToken(String authHeader) {
        return JwtUtil.extractToken(authHeader, jwtUtil.getTokenType());
    }

    /**
     * 从Claims中提取用户ID
     */
    private Long extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }
        return null;
    }

    /**
     * 从Claims中提取角色
     */
    private String extractRole(Claims claims) {
        Object role = claims.get("role");
        return role != null ? role.toString() : null;
    }
}