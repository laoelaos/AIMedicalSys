package com.aimedical.modules.commonmodule.controller;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.response.LoginResponse;
import com.aimedical.modules.commonmodule.dto.response.UserInfoResponse;
import com.aimedical.modules.commonmodule.jwt.JwtConfig;
import com.aimedical.modules.commonmodule.jwt.JwtUtil;
import com.aimedical.modules.commonmodule.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthController纯单元测试
 *
 * <p>不依赖Spring容器，直接测试Controller方法逻辑。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController测试")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private JwtUtil jwtUtil;
    private AuthController authController;

    private LoginResponse mockLoginResponse;

    @BeforeEach
    void setUp() {
        // 创建真实的JwtUtil实例
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("AIMedicalSysJwtSecretKey2026Phase1DevelopmentTestSecretKey");
        jwtConfig.setExpiration(86400L);
        jwtConfig.setTokenType("Bearer");
        jwtUtil = new JwtUtil(jwtConfig);

        authController = new AuthController(authService, jwtUtil);

        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId(1L);
        userInfo.setUsername("testuser");
        userInfo.setRealName("测试用户");
        userInfo.setRole("DOCTOR");
        userInfo.setPermissions(Collections.emptyList());

        mockLoginResponse = new LoginResponse();
        mockLoginResponse.setToken("mock-jwt-token");
        mockLoginResponse.setTokenType("Bearer");
        mockLoginResponse.setExpiresIn(86400L);
        mockLoginResponse.setUser(userInfo);
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("登录成功返回SUCCESS")
        void shouldReturnSuccessWhenLoginSucceeds() {
            when(authService.login(any(LoginRequest.class))).thenReturn(mockLoginResponse);

            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            Result<LoginResponse> result = authController.login(request);

            assertEquals("SUCCESS", result.getCode());
            assertEquals("mock-jwt-token", result.getData().getToken());
            assertEquals("Bearer", result.getData().getTokenType());
            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("登录失败返回null数据")
        void shouldReturnNullDataWhenLoginFails() {
            when(authService.login(any(LoginRequest.class))).thenReturn(null);

            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongpassword");

            Result<LoginResponse> result = authController.login(request);

            assertEquals("SUCCESS", result.getCode());
            assertNull(result.getData());
            verify(authService, times(1)).login(any(LoginRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("登出成功返回SUCCESS")
        void shouldReturnSuccessWhenLogoutSucceeds() {
            doNothing().when(authService).logout(any());

            Result<Void> result = authController.logout("Bearer mock-token");

            assertEquals("SUCCESS", result.getCode());
            verify(authService, times(1)).logout("mock-token");
        }

        @Test
        @DisplayName("无token登出也返回SUCCESS（静默处理）")
        void shouldReturnSuccessEvenWhenNoToken() {
            Result<Void> result = authController.logout(null);

            assertEquals("SUCCESS", result.getCode());
            verify(authService, never()).logout(any());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("刷新token成功返回SUCCESS")
        void shouldReturnSuccessWhenRefreshSucceeds() {
            when(authService.refreshToken("mock-token")).thenReturn(mockLoginResponse);

            Result<LoginResponse> result = authController.refresh("Bearer mock-token");

            assertEquals("SUCCESS", result.getCode());
            assertEquals("mock-jwt-token", result.getData().getToken());
            verify(authService, times(1)).refreshToken("mock-token");
        }

        @Test
        @DisplayName("无效token返回null数据")
        void shouldReturnNullDataWhenInvalidToken() {
            when(authService.refreshToken("invalid-token")).thenReturn(null);

            Result<LoginResponse> result = authController.refresh("Bearer invalid-token");

            assertEquals("SUCCESS", result.getCode());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("无token返回UNAUTHORIZED")
        void shouldReturnUnauthorizedWhenNoToken() {
            Result<LoginResponse> result = authController.refresh(null);

            assertEquals("UNAUTHORIZED", result.getCode());
            assertEquals("未提供令牌", result.getMessage());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class MeTests {

        @Test
        @DisplayName("获取当前用户成功返回SUCCESS")
        void shouldReturnSuccessWhenGetCurrentUserSucceeds() {
            UserInfoResponse userInfo = new UserInfoResponse();
            userInfo.setId(1L);
            userInfo.setUsername("testuser");
            userInfo.setRealName("测试用户");
            userInfo.setRole("DOCTOR");

            when(authService.getCurrentUser("mock-token")).thenReturn(userInfo);

            Result<UserInfoResponse> result = authController.me("Bearer mock-token");

            assertEquals("SUCCESS", result.getCode());
            assertEquals("testuser", result.getData().getUsername());
        }

        @Test
        @DisplayName("无效token返回null数据")
        void shouldReturnNullDataWhenInvalidToken() {
            when(authService.getCurrentUser("invalid-token")).thenReturn(null);

            Result<UserInfoResponse> result = authController.me("Bearer invalid-token");

            assertEquals("SUCCESS", result.getCode());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("无token返回UNAUTHORIZED")
        void shouldReturnUnauthorizedWhenNoToken() {
            Result<UserInfoResponse> result = authController.me(null);

            assertEquals("UNAUTHORIZED", result.getCode());
            assertEquals("未提供令牌", result.getMessage());
        }
    }
}
