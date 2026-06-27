package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class PasswordChangeCheckFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeCheckFilter.class);

    private final AntPathRequestMatcher passwordMatcher;
    private final AntPathRequestMatcher logoutMatcher;
    private final AntPathRequestMatcher refreshMatcher;

    PasswordChangeCheckFilter() {
        this.passwordMatcher = new AntPathRequestMatcher("/api/auth/password", "PUT");
        this.logoutMatcher = new AntPathRequestMatcher("/api/auth/logout", "POST");
        this.refreshMatcher = new AntPathRequestMatcher("/api/auth/refresh", "POST");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        Object passwordChangeRequired = request.getAttribute("passwordChangeRequired");
        if (!Boolean.TRUE.equals(passwordChangeRequired)) {
            chain.doFilter(request, response);
            return;
        }

        if (isWhitelisted(request)) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Password change required for userId={}, blocking request: {} {}",
                auth.getPrincipal(), request.getMethod(), request.getRequestURI());
        SecurityContextHolder.clearContext();
        throw new PasswordChangeRequiredException("Password change required");
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        return passwordMatcher.matches(request)
                || logoutMatcher.matches(request)
                || refreshMatcher.matches(request);
    }
}
