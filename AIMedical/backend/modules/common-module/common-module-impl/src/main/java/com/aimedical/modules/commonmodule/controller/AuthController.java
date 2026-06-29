package com.aimedical.modules.commonmodule.controller;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.dto.ProfileUpdateRequest;
import com.aimedical.modules.commonmodule.api.dto.TokenRefreshResponse;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;
import com.aimedical.modules.commonmodule.auth.UserInfoResponse;
import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequest;
import com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequest;
import com.aimedical.modules.commonmodule.permission.UserRepository;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
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
@Profile("phase1")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.authenticate(request.username(), request.password());
        return Result.success(response);
    }

    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        if (token != null) {
            authService.logout(token);
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
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.aimedical.common.exception.BusinessException(
                    com.aimedical.common.exception.GlobalErrorCode.UNAUTHORIZED, "未认证");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long uid) return uid;
        if (principal instanceof Integer uid) return uid.longValue();
        // Principal is username (String) — look up userId
        if (principal instanceof String username) {
            return userRepository.findByUsername(username)
                    .map(u -> u.getId())
                    .orElseThrow(() -> new com.aimedical.common.exception.BusinessException(
                            com.aimedical.common.exception.GlobalErrorCode.UNAUTHORIZED, "用户不存在"));
        }
        throw new com.aimedical.common.exception.BusinessException(
                com.aimedical.common.exception.GlobalErrorCode.UNAUTHORIZED, "无法识别用户身份");
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) return null;
        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return null;
    }
}
