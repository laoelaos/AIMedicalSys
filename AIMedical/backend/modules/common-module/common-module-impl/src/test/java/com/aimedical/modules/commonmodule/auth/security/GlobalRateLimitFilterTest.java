package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class GlobalRateLimitFilterTest {

    private final SlidingWindowCounter counter = new SlidingWindowCounter();
    private final GlobalRateLimitFilter filter = new GlobalRateLimitFilter(counter);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPassRequestWithinLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/user/me");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldBlockRequestWhenLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/user/me");
        request.setRemoteAddr("192.168.1.1");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 100; i++) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(request, resp, chain);
            assertEquals(200, resp.getStatus());
        }
        MockHttpServletResponse blockedResp = new MockHttpServletResponse();
        filter.doFilterInternal(request, blockedResp, chain);
        assertEquals(429, blockedResp.getStatus());
        verify(chain, times(100)).doFilter(any(), any());
    }

    @Test
    void shouldPassWhitelistedLoginPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("192.168.1.1");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 101; i++) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(request, resp, chain);
            assertEquals(200, resp.getStatus());
        }
        verify(chain, times(101)).doFilter(any(), any());
    }

    @Test
    void shouldPassWhitelistedRefreshPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/refresh");
        request.setRemoteAddr("192.168.1.1");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 101; i++) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(request, resp, chain);
            assertEquals(200, resp.getStatus());
        }
        verify(chain, times(101)).doFilter(any(), any());
    }

    @Test
    void shouldPassWhitelistedActuatorHealthPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");
        request.setRemoteAddr("192.168.1.1");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 101; i++) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(request, resp, chain);
            assertEquals(200, resp.getStatus());
        }
        verify(chain, times(101)).doFilter(any(), any());
    }

    @Test
    void shouldPassWhitelistedActuatorInfoPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/info");
        request.setRemoteAddr("192.168.1.1");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 101; i++) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(request, resp, chain);
            assertEquals(200, resp.getStatus());
        }
        verify(chain, times(101)).doFilter(any(), any());
    }

    @Test
    void shouldHandleDifferentIpsIndependently() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest requestA = new MockHttpServletRequest();
        requestA.setRequestURI("/api/user/me");
        requestA.setRemoteAddr("192.168.1.1");

        for (int i = 0; i < 100; i++) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(requestA, resp, chain);
            assertEquals(200, resp.getStatus());
        }
        MockHttpServletResponse blockedResp = new MockHttpServletResponse();
        filter.doFilterInternal(requestA, blockedResp, chain);
        assertEquals(429, blockedResp.getStatus());

        MockHttpServletRequest requestB = new MockHttpServletRequest();
        requestB.setRequestURI("/api/user/me");
        requestB.setRemoteAddr("192.168.1.2");
        MockHttpServletResponse respB = new MockHttpServletResponse();
        filter.doFilterInternal(requestB, respB, chain);
        assertEquals(200, respB.getStatus());

        verify(chain, times(101)).doFilter(any(), any());
    }

    @Test
    void shouldReturnRateLimitExceededResponseBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/user/me");
        request.setRemoteAddr("192.168.1.1");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 100; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse blockedResp = new MockHttpServletResponse();
        filter.doFilterInternal(request, blockedResp, chain);

        assertEquals("application/json", blockedResp.getContentType());
        JsonNode body = objectMapper.readTree(blockedResp.getContentAsString());
        assertEquals("RATE_LIMITED_GLOBAL", body.get("code").asText());
    }

    @Test
    void shouldUseXForwardedForHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/user/me");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.setRemoteAddr("192.168.1.1");
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 100; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse blockedResp = new MockHttpServletResponse();
        filter.doFilterInternal(request, blockedResp, chain);
        assertEquals(429, blockedResp.getStatus());
    }
}
