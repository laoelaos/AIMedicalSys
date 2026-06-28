package com.aimedical.modules.commonmodule.controller;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.dto.TokenRefreshResponse;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;
import com.aimedical.modules.commonmodule.auth.UserInfoResponse;
import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequest;
import com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequest;

import com.aimedical.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController测试")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    private TokenResponse mockTokenResponse;
    private UserInfoResponse mockUserInfo;
    private TokenRefreshResponse mockRefreshResponse;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);

        mockUserInfo = new UserInfoResponse(1L, "testuser", "测试用户",
                null, null, "DOCTOR", null, Set.of());

        mockTokenResponse = new TokenResponse("mock-jwt-token", "mock-refresh-token", 86400L);

        mockRefreshResponse = new TokenRefreshResponse("new-token", "new-refresh-token", "Bearer", 86400L);
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("登录成功返回SUCCESS")
        void shouldReturnSuccessWhenLoginSucceeds() {
            when(authService.authenticate(anyString(), anyString())).thenReturn(mockTokenResponse);

            var request = new LoginRequest("testuser", "password123");
            Result<TokenResponse> result = authController.login(request);

            assertEquals("SUCCESS", result.getCode());
            assertEquals("mock-jwt-token", result.getData().getAccessToken());
            verify(authService, times(1)).authenticate("testuser", "password123");
        }

        @Test
        @DisplayName("登录失败抛出BusinessException（用户名或密码错误）")
        void shouldThrowBusinessExceptionWhenLoginFails() {
            when(authService.authenticate(anyString(), anyString()))
                    .thenThrow(new BusinessException(GlobalErrorCode.UNAUTHORIZED, "用户名或密码错误"));

            var request = new LoginRequest("testuser", "wrongpassword");
            assertThrows(BusinessException.class, () -> authController.login(request));
            verify(authService, times(1)).authenticate(eq("testuser"), eq("wrongpassword"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("登出成功返回SUCCESS")
        void shouldReturnSuccessWhenLogoutSucceeds() {
            doNothing().when(authService).logout(anyString());

            Result<Void> result = authController.logout("Bearer mock-token");

            assertEquals("SUCCESS", result.getCode());
            verify(authService, times(1)).logout(eq("mock-token"));
        }

        @Test
        @DisplayName("无token登出也返回SUCCESS（静默处理）")
        void shouldReturnSuccessEvenWhenNoToken() {
            Result<Void> result = authController.logout(null);

            assertEquals("SUCCESS", result.getCode());
            verify(authService, never()).logout(anyString());
        }

        @Test
        @DisplayName("空字符串token登出返回SUCCESS（静默处理）")
        void shouldReturnSuccessWhenEmptyToken() {
            Result<Void> result = authController.logout("");

            assertEquals("SUCCESS", result.getCode());
            verify(authService, never()).logout(anyString());
        }

        @Test
        @DisplayName("非Bearer前缀token登出返回SUCCESS（静默处理）")
        void shouldReturnSuccessWhenNonBearerToken() {
            Result<Void> result = authController.logout("Basic abc123");

            assertEquals("SUCCESS", result.getCode());
            verify(authService, never()).logout(anyString());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("刷新token成功返回SUCCESS")
        void shouldReturnSuccessWhenRefreshSucceeds() {
            when(authService.refreshToken("valid-refresh-token")).thenReturn(mockRefreshResponse);

            var request = new RefreshTokenRequest("valid-refresh-token");
            Result<TokenRefreshResponse> result = authController.refresh(request);

            assertEquals("SUCCESS", result.getCode());
            assertEquals("new-token", result.getData().accessToken());
            verify(authService, times(1)).refreshToken("valid-refresh-token");
        }

        @Test
        @DisplayName("无效token抛出BusinessException（令牌无效）")
        void shouldThrowBusinessExceptionWhenInvalidToken() {
            when(authService.refreshToken("invalid-token"))
                    .thenThrow(new BusinessException(GlobalErrorCode.UNAUTHORIZED, "令牌无效"));

            var request = new RefreshTokenRequest("invalid-token");
            assertThrows(BusinessException.class, () -> authController.refresh(request));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class MeTests {

        @Test
        @DisplayName("获取当前用户成功返回SUCCESS")
        void shouldReturnSuccessWhenGetCurrentUserSucceeds() {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(1L);
            SecurityContextHolder.setContext(securityContext);

            when(authService.getCurrentUser(1L)).thenReturn(mockUserInfo);

            Result<UserInfoResponse> result = authController.me();

            assertEquals("SUCCESS", result.getCode());
            assertEquals("testuser", result.getData().username());
            verify(authService).getCurrentUser(1L);
        }

        @Test
        @DisplayName("SecurityContext无Authentication时抛出IllegalStateException")
        void shouldThrowWhenNoAuthentication() {
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            SecurityContextHolder.setContext(securityContext);

            assertThrows(BusinessException.class, () -> authController.me());
        }

        @Test
        @DisplayName("principal类型非Long/Integer时抛出IllegalStateException")
        void shouldThrowWhenPrincipalTypeInvalid() {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("not-a-number");
            SecurityContextHolder.setContext(securityContext);

            assertThrows(BusinessException.class, () -> authController.me());
        }
    }

    @Nested
    @DisplayName("PUT /api/auth/profile")
    class UpdateMeTests {

        @Test
        @DisplayName("更新个人资料成功返回SUCCESS")
        void shouldReturnSuccessWhenUpdateProfileSucceeds() {
            when(authService.updateProfile(anyString(), any()))
                    .thenReturn(mockUserInfo);

            var request = new com.aimedical.modules.commonmodule.api.dto.ProfileUpdateRequest(
                    "新昵称", null, null);
            Result<UserInfoResponse> result = authController.updateMe("Bearer mock-token", request);

            assertEquals("SUCCESS", result.getCode());
            verify(authService, times(1)).updateProfile(anyString(), any());
        }

        @Test
        @DisplayName("无token返回UNAUTHORIZED")
        void shouldReturnUnauthorizedWhenNoToken() {
            var request = new com.aimedical.modules.commonmodule.api.dto.ProfileUpdateRequest(
                    "昵称", null, null);
            Result<UserInfoResponse> result = authController.updateMe(null, request);

            assertEquals("UNAUTHORIZED", result.getCode());
            assertEquals("未提供令牌", result.getMessage());
        }
    }

    @Nested
    @DisplayName("PUT /api/auth/password")
    class ChangePasswordTests {

        @Test
        @DisplayName("密码修改端点返回SUCCESS")
        void shouldReturnSuccessForChangePassword() {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(1L);
            SecurityContextHolder.setContext(securityContext);

            doNothing().when(authService).changePassword(anyLong(), anyString(), anyString());

            var request = new PasswordChangeRequest("oldPass", "newPass123");
            Result<Void> result = authController.changePassword(request);

            assertEquals("SUCCESS", result.getCode());
            verify(authService).changePassword(1L, "oldPass", "newPass123");
        }

        @Test
        @DisplayName("密码修改失败抛出BusinessException（旧密码不正确）")
        void shouldThrowBusinessExceptionWhenChangePasswordFails() {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(1L);
            SecurityContextHolder.setContext(securityContext);

            doThrow(new BusinessException(GlobalErrorCode.PASSWORD_MISMATCH))
                    .when(authService).changePassword(anyLong(), anyString(), anyString());

            var request = new PasswordChangeRequest("wrongOldPass", "newPass123");
            assertThrows(BusinessException.class, () -> authController.changePassword(request));
            verify(authService).changePassword(1L, "wrongOldPass", "newPass123");
        }
    }
}
