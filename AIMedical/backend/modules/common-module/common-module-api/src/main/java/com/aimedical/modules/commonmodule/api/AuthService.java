package com.aimedical.modules.commonmodule.api;

import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.commonmodule.api.dto.LoginRequest;
import com.aimedical.modules.commonmodule.api.dto.ProfileUpdateRequest;
import com.aimedical.modules.commonmodule.api.dto.RegisterRequest;
import com.aimedical.modules.commonmodule.api.dto.TokenRefreshResponse;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;
import com.aimedical.modules.commonmodule.auth.UserInfoResponse;

/**
 * Unified authentication service interface.
 *
 * <p>Provides registration, login, token refresh, logout, current-user lookup,
 * profile update, and password change for all user types (patient / doctor / admin).
 */
public interface AuthService {

    // ── Patient-facing (phone-based) ────────────────────────────────────

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    void logout(String accessToken);

    CurrentUserResponse getCurrentUser();

    // ── Doctor/Admin (username-based) ───────────────────────────────────

    /** Authenticate by username+password, return token pair. */
    TokenResponse authenticate(String username, String password);

    /** Refresh token — returns a new access-token + refresh-token pair. */
    TokenRefreshResponse refreshToken(String refreshToken);

    /** Look up current user by explicit userId (for use without SecurityContext). */
    UserInfoResponse getCurrentUser(Long userId);

    /** Update profile fields (nickname, phone, email). */
    UserInfoResponse updateProfile(String token, ProfileUpdateRequest request);

    /** Change password — validates old password before applying the new one. */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
