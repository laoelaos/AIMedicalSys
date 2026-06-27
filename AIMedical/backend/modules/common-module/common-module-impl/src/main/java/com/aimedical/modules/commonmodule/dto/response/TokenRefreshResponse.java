package com.aimedical.modules.commonmodule.dto.response;

public record TokenRefreshResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {}
