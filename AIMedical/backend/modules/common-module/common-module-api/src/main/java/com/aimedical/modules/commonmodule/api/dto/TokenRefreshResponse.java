package com.aimedical.modules.commonmodule.api.dto;

public record TokenRefreshResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {}
