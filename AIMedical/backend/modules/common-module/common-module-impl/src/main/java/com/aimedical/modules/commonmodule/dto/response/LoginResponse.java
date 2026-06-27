package com.aimedical.modules.commonmodule.dto.response;

import com.aimedical.modules.commonmodule.auth.UserInfoResponse;

public record LoginResponse(
    Long userId,
    String username,
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    boolean passwordChangeRequired,
    UserInfoResponse user
) {}
