package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;
import com.aimedical.modules.commonmodule.jwt.JwtUtil;
import com.aimedical.modules.commonmodule.permission.PermissionFunction;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final UserRepository userRepository;

    JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, TokenBlacklist tokenBlacklist, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractToken(authHeader);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        Claims claims = jwtTokenProvider.validateToken(token, "access");
        if (claims == null) {
            log.debug("JWT token validation failed");
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
            return;
        }

        String jti = extractJti(claims);
        if (jti != null && tokenBlacklist.isBlacklisted(jti)) {
            log.debug("Token is blacklisted, jti={}", jti);
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
            return;
        }

        Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
        if (userId == null) {
            log.debug("Failed to extract userId from claims");
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
            return;
        }

        Optional<User> userOpt = userRepository.findWithDetailsById(userId);
        if (userOpt.isEmpty()) {
            log.debug("User not found or deleted, userId={}", userId);
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
            return;
        }
        User user = userOpt.get();

        if (!user.getEnabled()) {
            log.warn("Account disabled, userId={}", userId);
            throwAccountDisabled(GlobalErrorCode.ACCOUNT_DISABLED.getMessage());
        }

        request.setAttribute("passwordChangeRequired", user.getPasswordChangeRequired());
        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUsername", user.getUsername());

        Collection<SimpleGrantedAuthority> authorities = collectAuthorities(user);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }

    private String extractToken(String authHeader) {
        return JwtUtil.extractToken(authHeader, jwtTokenProvider.getTokenType());
    }

    private String extractJti(Claims claims) {
        return claims.get("jti", String.class);
    }

    private Collection<SimpleGrantedAuthority> collectAuthorities(User user) {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
        }
        for (Post post : user.getPosts()) {
            for (PermissionFunction func : post.getFunctions()) {
                authorities.add(new SimpleGrantedAuthority("FUNC_" + func.getCode()));
            }
        }
        return authorities;
    }

    private void throwAccountDisabled(String message) {
        throw new AccountDisabledAuthenticationException(message);
    }

}
