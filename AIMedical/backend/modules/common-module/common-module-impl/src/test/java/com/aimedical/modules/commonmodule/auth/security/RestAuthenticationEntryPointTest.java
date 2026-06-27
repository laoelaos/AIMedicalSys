package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.util.MessageInterpolator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationException;
import org.springframework.security.core.AuthenticationException;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestAuthenticationEntryPointTest {

    private final MessageInterpolator messageInterpolator = mock(MessageInterpolator.class);
    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(messageInterpolator);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(messageInterpolator.interpolate(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldReturnAccountDisabledWhenMessageMatches() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new AccountDisabledAuthenticationException("any message");

        entryPoint.commence(request, response, authException);

        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals("ACCOUNT_DISABLED", body.get("code").asText());
        assertEquals("账户已被管理员停用", body.get("message").asText());
    }

    @Test
    void shouldReturnUnauthorizedForGenericException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = mock(AuthenticationException.class);
        when(authException.getMessage()).thenReturn("some other error");

        entryPoint.commence(request, response, authException);

        assertEquals(401, response.getStatus());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals("UNAUTHORIZED", body.get("code").asText());
        assertEquals("未认证或令牌已失效", body.get("message").asText());
    }

    @Test
    void shouldReturnUnauthorizedWhenMessageIsNull() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = mock(AuthenticationException.class);
        when(authException.getMessage()).thenReturn(null);

        entryPoint.commence(request, response, authException);

        assertEquals(401, response.getStatus());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals("UNAUTHORIZED", body.get("code").asText());
        assertEquals("未认证或令牌已失效", body.get("message").asText());
    }
}
