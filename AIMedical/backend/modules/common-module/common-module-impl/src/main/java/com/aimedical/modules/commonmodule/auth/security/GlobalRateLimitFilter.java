package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private static final int RATE_LIMIT = 100;
    private static final long RATE_WINDOW_MS = 60_000L;
    private static final List<String> WHITELIST_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator/health",
            "/actuator/info"
    );

    private final SlidingWindowCounter counter;
    private final ObjectMapper objectMapper;

    public GlobalRateLimitFilter(SlidingWindowCounter counter) {
        this.counter = counter;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (WHITELIST_PATHS.contains(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String forwardedHeader = request.getHeader("X-Forwarded-For");
        String ip = (forwardedHeader != null && !forwardedHeader.isEmpty())
                ? forwardedHeader.split(",")[0].trim()
                : request.getRemoteAddr();

        boolean allowed = counter.tryAcquire(ip, RATE_LIMIT, RATE_WINDOW_MS);
        if (allowed) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            String body = objectMapper.writeValueAsString(Result.fail(GlobalErrorCode.RATE_LIMITED_GLOBAL));
            response.getWriter().write(body);
        }
    }
}
