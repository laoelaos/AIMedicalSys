package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.common.util.MessageInterpolator;
import com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final MessageInterpolator messageInterpolator;

    public RestAccessDeniedHandler(MessageInterpolator messageInterpolator) {
        this.objectMapper = new ObjectMapper();
        this.messageInterpolator = messageInterpolator;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        GlobalErrorCode errorCode = (accessDeniedException instanceof PasswordChangeRequiredException)
                ? GlobalErrorCode.PASSWORD_CHANGE_REQUIRED
                : GlobalErrorCode.FORBIDDEN;

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
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
