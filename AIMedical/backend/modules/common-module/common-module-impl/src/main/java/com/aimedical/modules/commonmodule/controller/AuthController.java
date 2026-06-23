package com.aimedical.modules.commonmodule.controller;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.JwtUtil;
import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.response.LoginResponse;
import com.aimedical.modules.commonmodule.dto.response.UserInfoResponse;
import com.aimedical.modules.commonmodule.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 *
 * <p>提供用户认证相关的REST API接口。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 用户登出
     *
     * @param authHeader Authorization请求头
     * @return 成功响应
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        if (token != null) {
            authService.logout(token);
        }
        return Result.success(null);
    }

    /**
     * 刷新令牌
     *
     * @param authHeader Authorization请求头
     * @return 新的登录响应
     */
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            return Result.fail("UNAUTHORIZED", "未提供令牌");
        }
        LoginResponse response = authService.refreshToken(token);
        return Result.success(response);
    }

    /**
     * 获取当前用户信息
     *
     * @param authHeader Authorization请求头
     * @return 用户信息响应
     */
    @GetMapping("/me")
    public Result<UserInfoResponse> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            return Result.fail("UNAUTHORIZED", "未提供令牌");
        }
        UserInfoResponse response = authService.getCurrentUser(token);
        return Result.success(response);
    }

    /**
     * 从Authorization头中提取JWT令牌
     *
     * @param authHeader Authorization请求头
     * @return JWT令牌，如果无效则返回null
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }
        String tokenType = jwtUtil.getTokenType();
        if (authHeader.startsWith(tokenType + " ")) {
            return authHeader.substring(tokenType.length() + 1);
        }
        return authHeader;
    }
}