package com.aimedical.modules.commonmodule.service;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.commonmodule.jwt.JwtConfig;
import com.aimedical.modules.commonmodule.jwt.JwtUtil;
import com.aimedical.modules.commonmodule.api.UserType;
import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.response.LoginResponse;
import com.aimedical.modules.commonmodule.dto.response.UserInfoResponse;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.aimedical.modules.commonmodule.service.impl.AuthServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthService单元测试
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService测试")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("AIMedicalSysJwtSecretKey2026Phase1DevelopmentTestSecretKey");
        jwtConfig.setExpiration(86400L);
        jwtConfig.setTokenType("Bearer");
        this.jwtUtil = new JwtUtil(jwtConfig);

        authService = new AuthServiceImpl(userRepository, passwordEncoder, this.jwtUtil);

        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setNickname("测试用户");
        testUser.setUserType(UserType.DOCTOR);
        testUser.setEnabled(true);
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("登录成功")
        void shouldLoginSuccessfully() {
            when(userRepository.findByUsername("testuser")).thenReturn(testUser);

            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            LoginResponse response = authService.login(request);

            assertNotNull(response);
            assertNotNull(response.getToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(86400L, response.getExpiresIn());
            assertNotNull(response.getUser());
            assertEquals("testuser", response.getUser().getUsername());
            assertEquals("测试用户", response.getUser().getRealName());
        }

        @Test
        @DisplayName("用户不存在抛出异常")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(null);

            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent");
            request.setPassword("password123");

            BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(request));

            assertEquals(GlobalErrorCode.UNAUTHORIZED, exception.getErrorCode());
        }

        @Test
        @DisplayName("密码错误抛出异常")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            when(userRepository.findByUsername("testuser")).thenReturn(testUser);

            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongpassword");

            BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(request));

            assertEquals(GlobalErrorCode.UNAUTHORIZED, exception.getErrorCode());
        }

        @Test
        @DisplayName("用户已禁用抛出异常")
        void shouldThrowExceptionWhenUserDisabled() {
            testUser.setEnabled(false);
            when(userRepository.findByUsername("testuser")).thenReturn(testUser);

            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(request));

            assertEquals(GlobalErrorCode.FORBIDDEN, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("登出成功（无状态JWT）")
        void shouldLogoutSuccessfully() {
            assertDoesNotThrow(() -> authService.logout("any-token"));
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("刷新token成功")
        void shouldRefreshTokenSuccessfully() {
            String oldToken = jwtUtil.generateToken(1L, "testuser", "DOCTOR", null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            LoginResponse response = authService.refreshToken(oldToken);

            assertNotNull(response);
            assertNotNull(response.getToken());
            assertEquals("Bearer", response.getTokenType());
        }

        @Test
        @DisplayName("无效token抛出异常")
        void shouldThrowExceptionForInvalidToken() {
            BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refreshToken("invalid-token"));

            assertEquals(GlobalErrorCode.UNAUTHORIZED, exception.getErrorCode());
        }

        @Test
        @DisplayName("用户不存在抛出异常")
        void shouldThrowExceptionWhenUserNotFound() {
            String token = jwtUtil.generateToken(1L, "testuser", "DOCTOR", null);
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refreshToken(token));

            assertEquals(GlobalErrorCode.NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUserTests {

        @Test
        @DisplayName("获取当前用户成功")
        void shouldGetCurrentUserSuccessfully() {
            String token = jwtUtil.generateToken(1L, "testuser", "DOCTOR", null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            UserInfoResponse response = authService.getCurrentUser(token);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("testuser", response.getUsername());
        }

        @Test
        @DisplayName("无效token抛出异常")
        void shouldThrowExceptionForInvalidToken() {
            BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.getCurrentUser("invalid-token"));

            assertEquals(GlobalErrorCode.UNAUTHORIZED, exception.getErrorCode());
        }

        @Test
        @DisplayName("用户不存在抛出异常")
        void shouldThrowExceptionWhenUserNotFound() {
            String token = jwtUtil.generateToken(1L, "testuser", "DOCTOR", null);
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.getCurrentUser(token));

            assertEquals(GlobalErrorCode.NOT_FOUND, exception.getErrorCode());
        }
    }
}