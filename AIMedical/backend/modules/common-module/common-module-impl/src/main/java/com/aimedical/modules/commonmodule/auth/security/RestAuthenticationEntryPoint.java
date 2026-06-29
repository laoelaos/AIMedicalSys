package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.common.util.MessageInterpolator;
import com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final MessageInterpolator messageInterpolator;

    public RestAuthenticationEntryPoint(MessageInterpolator messageInterpolator) {
        this.objectMapper = new ObjectMapper();
        this.messageInterpolator = messageInterpolator;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        boolean isAccountDisabled = authException instanceof AccountDisabledAuthenticationException;
        GlobalErrorCode errorCode = isAccountDisabled ? GlobalErrorCode.ACCOUNT_DISABLED : GlobalErrorCode.UNAUTHORIZED;

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            String message = messageInterpolator.interpolate(errorCode.getMessage(), null);
            String body = objectMapper.writeValueAsString(Result.fail(errorCode.getCode(), message));
            response.getWriter().write(body);
        } catch (JsonProcessingException e) {
            response.getWriter().write("{\"code\":\"SYSTEM_ERROR\",\"message\":\"系统异常\"}");
        }
    }
}
