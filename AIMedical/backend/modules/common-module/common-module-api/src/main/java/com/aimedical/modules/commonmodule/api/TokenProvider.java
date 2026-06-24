package com.aimedical.modules.commonmodule.api;

import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;

/**
 * Token provider interface — implemented by the application module (JWT).
 * common-module-impl depends on this interface, not on any JWT library.
 */
public interface TokenProvider {

    TokenResponse generateTokens(Long userId, String username);

    String generateAccessToken(Long userId, String username);

    String generateRefreshToken(Long userId, String username);

    boolean validateToken(String token);

    String getUsernameFromToken(String token);

    CurrentUserResponse getCurrentUserFromToken(String token);

    void invalidateToken(String token);
}
