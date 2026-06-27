package com.aimedical.modules.commonmodule.service;

import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.LoginResponse;
import com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponse;
import com.aimedical.modules.commonmodule.auth.UserInfoResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(String token, String refreshToken);

    TokenRefreshResponse refreshToken(String token);

    UserInfoResponse getCurrentUser(Long userId);

    UserInfoResponse updateProfile(String token, ProfileUpdateRequest request);

    void changePassword(Long userId, String oldPassword, String newPassword);
}
