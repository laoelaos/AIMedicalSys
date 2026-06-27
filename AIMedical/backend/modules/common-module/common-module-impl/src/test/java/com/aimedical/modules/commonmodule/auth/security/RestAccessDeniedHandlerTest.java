package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.util.MessageInterpolator;
import com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestAccessDeniedHandlerTest {

    private final MessageInterpolator messageInterpolator = mock(MessageInterpolator.class);
    private final RestAccessDeniedHandler deniedHandler = new RestAccessDeniedHandler(messageInterpolator);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(messageInterpolator.interpolate(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldReturnPasswordChangeRequiredForPasswordChangeException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        PasswordChangeRequiredException ex = new PasswordChangeRequiredException("需要修改密码");

        deniedHandler.handle(request, response, ex);

        assertEquals(403, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals("PASSWORD_CHANGE_REQUIRED", body.get("code").asText());
        assertEquals("需要修改密码", body.get("message").asText());
    }

    @Test
    void shouldReturnForbiddenForGenericAccessDenied() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException ex = mock(AccessDeniedException.class);
        when(ex.getMessage()).thenReturn("access denied");

        deniedHandler.handle(request, response, ex);

        assertEquals(403, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals("FORBIDDEN", body.get("code").asText());
        assertEquals("无权限访问", body.get("message").asText());
    }
}
