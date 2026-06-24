package com.aimedical.modules.commonmodule.api;

import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.commonmodule.api.dto.LoginRequest;
import com.aimedical.modules.commonmodule.api.dto.RegisterRequest;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;

/**
 * Authentication service interface — implemented by common-module-impl.
 * Provides registration, login, token refresh, and logout for all user types.
 */
public interface AuthService {

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(String refreshToken);

    void logout(String accessToken);

    CurrentUserResponse getCurrentUser();
}
