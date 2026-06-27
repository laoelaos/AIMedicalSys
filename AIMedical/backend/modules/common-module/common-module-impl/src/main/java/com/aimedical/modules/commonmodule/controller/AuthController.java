package com.aimedical.modules.commonmodule.controller;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequest;
import com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequest;
import com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequest;
import com.aimedical.modules.commonmodule.dto.response.LoginResponse;
import com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponse;
import com.aimedical.modules.commonmodule.auth.UserInfoResponse;
import com.aimedical.modules.commonmodule.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest) {
        String token = extractToken(authHeader);
        if (token != null) {
            authService.logout(token, refreshTokenRequest != null ? refreshTokenRequest.refreshToken() : null);
        }
        return Result.success(null);
    }

    @PostMapping("/refresh")
    public Result<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request.refreshToken());
        return Result.success(response);
    }

    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        Long userId = getCurrentUserId();
        UserInfoResponse response = authService.getCurrentUser(userId);
        return Result.success(response);
    }

    @PutMapping("/profile")
    public Result<UserInfoResponse> updateMe(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ProfileUpdateRequest request) {
        String token = extractToken(authHeader);
        if (token == null) {
            return Result.fail("UNAUTHORIZED", "未提供令牌");
        }
        UserInfoResponse response = authService.updateProfile(token, request);
        return Result.success(response);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(
            @Valid @RequestBody PasswordChangeRequest request) {
        Long userId = getCurrentUserId();
        authService.changePassword(userId, request.oldPassword(), request.newPassword());
        return Result.success(null);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("无法从SecurityContext获取用户ID");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        if (principal instanceof Integer) {
            return ((Integer) principal).longValue();
        }
        throw new IllegalStateException("无法从SecurityContext获取用户ID");
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) return null;
        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return null;
    }
}
