package com.aimedical.modules.commonmodule.service;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.commonmodule.auth.UserInfoResponse;
import com.aimedical.modules.commonmodule.auth.audit.SecurityAuditEvent;
import com.aimedical.modules.commonmodule.auth.audit.SecurityAuditEventType;
import com.aimedical.modules.commonmodule.auth.audit.SecurityAuditLogger;
import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
import com.aimedical.modules.commonmodule.auth.converter.UserConverter;
import com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredException;
import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;
import com.aimedical.modules.commonmodule.auth.login.LoginAttemptTracker;
import com.aimedical.modules.commonmodule.auth.password.PasswordChangeService;
import com.aimedical.modules.commonmodule.auth.password.PasswordPolicy;
import com.aimedical.modules.commonmodule.auth.rateLimit.RateLimitGuard;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;
import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.api.dto.ProfileUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.LoginResponse;
import com.aimedical.modules.commonmodule.api.dto.TokenRefreshResponse;
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.RoleRepository;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.aimedical.modules.commonmodule.service.impl.AuthServiceImpl;

import io.jsonwebtoken.Claims;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock UserConverter userConverter;
    @Mock PasswordPolicy passwordPolicy;
    @Mock PasswordChangeService passwordChangeService;
    @Mock RateLimitGuard rateLimitGuard;
    @Mock TokenBlacklist tokenBlacklist;
    @Mock LoginAttemptTracker loginAttemptTracker;
    @Mock SecurityAuditLogger securityAuditLogger;

    private AuthServiceImpl authService;
    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, roleRepository, passwordEncoder, jwtTokenProvider, userConverter,
                passwordPolicy, passwordChangeService, rateLimitGuard,
                tokenBlacklist, loginAttemptTracker, securityAuditLogger);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$hashedpassword");
        testUser.setNickname("TestUser");
        testUser.setUserType(com.aimedical.modules.commonmodule.api.UserType.DOCTOR);
        testUser.setEnabled(true);
        testUser.setDeleted(false);
        testUser.setTokenVersion(1);
        testUser.setPasswordChangeRequired(false);
    }

    @Test
    void login_shouldSucceed() {
        when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(loginAttemptTracker.isIpLocked(anyString())).thenReturn(false);
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordChangeService.isChangeRequired(anyLong())).thenReturn(false);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString(), anyString(), anyInt())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(userConverter.toUserInfoResponse(any())).thenReturn(
                new UserInfoResponse(1L, "testuser", "TestUser", null, null, "DOCTOR", null, java.util.Set.of()));

        TokenResponse response = authService.authenticate("testuser", "password");

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(900000L, response.getExpiresIn());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGIN_SUCCESS, event.eventType());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertTrue(event.success());
        assertNull(event.failureReason());
        assertNull(event.refreshTokenMasked());
        assertNull(event.newJti());
    }

    @Test
    void login_shouldThrowRateLimited() {
        when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.authenticate("testuser", "password"));
        assertEquals(GlobalErrorCode.RATE_LIMITED, ex.getErrorCode());
    }

    @Test
    void login_shouldThrowIpLocked() {
        when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(loginAttemptTracker.isIpLocked(anyString())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.authenticate("testuser", "password"));
        assertEquals(GlobalErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGIN_FAILED, event.eventType());
        assertEquals("IP_LOCKED", event.failureReason());
        assertNull(event.userId());
        assertNull(event.username());
        assertFalse(event.success());
        assertEquals("30分钟", ex.getArgs()[0]);
    }

    @Test
    void login_shouldThrowUsernameLocked() {
        when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(loginAttemptTracker.isIpLocked(anyString())).thenReturn(false);
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.authenticate("testuser", "password"));
        assertEquals(GlobalErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGIN_FAILED, event.eventType());
        assertEquals("USERNAME_LOCKED", event.failureReason());
        assertNull(event.userId());
        assertNull(event.username());
        assertFalse(event.success());
        assertEquals("15分钟", ex.getArgs()[0]);
    }

    @Test
    void login_shouldThrowUserNotFound() {
        when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(loginAttemptTracker.isIpLocked(anyString())).thenReturn(false);
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.authenticate("nonexistent", "password"));
        assertEquals(GlobalErrorCode.LOGIN_FAILED, ex.getErrorCode());
        verify(passwordEncoder).matches(eq("dummy"), anyString());
        verify(loginAttemptTracker).recordIpFailure(anyString());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGIN_FAILED, event.eventType());
        assertEquals("USER_NOT_FOUND", event.failureReason());
        assertNull(event.userId());
        assertNull(event.username());
        assertFalse(event.success());
    }

    @Test
    void login_shouldThrowUserDisabled() {
        testUser.setEnabled(false);
        when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(loginAttemptTracker.isIpLocked(anyString())).thenReturn(false);
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.authenticate("testuser", "password"));
        assertEquals(GlobalErrorCode.LOGIN_FAILED, ex.getErrorCode());
        verify(passwordEncoder).matches(eq("dummy"), anyString());
        verify(loginAttemptTracker).recordIpFailure(anyString());
        verify(loginAttemptTracker).recordUsernameFailure(anyString());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGIN_FAILED, event.eventType());
        assertEquals("ACCOUNT_DISABLED", event.failureReason());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertFalse(event.success());
    }

    @Test
    void login_shouldThrowPasswordMismatch() {
        when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(loginAttemptTracker.isIpLocked(anyString())).thenReturn(false);
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.authenticate("testuser", "wrong"));
        assertEquals(GlobalErrorCode.LOGIN_FAILED, ex.getErrorCode());
        verify(loginAttemptTracker).recordUsernameFailure("testuser");

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGIN_FAILED, event.eventType());
        assertEquals("BAD_CREDENTIALS", event.failureReason());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertFalse(event.success());
    }

    @Test
    void login_shouldSetPasswordChangeRequired() {
        when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(loginAttemptTracker.isIpLocked(anyString())).thenReturn(false);
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordChangeService.isChangeRequired(anyLong())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString(), anyString(), anyInt())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(userConverter.toUserInfoResponse(any())).thenReturn(
                new UserInfoResponse(1L, "testuser", "TestUser", null, null, "DOCTOR", null, java.util.Set.of()));

        TokenResponse response = authService.authenticate("testuser", "password");

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGIN_SUCCESS, event.eventType());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertTrue(event.success());
    }

    @Test
    void login_shouldThrowUserDeleted() {
        testUser.setDeleted(true);
        when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(loginAttemptTracker.isIpLocked(anyString())).thenReturn(false);
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.authenticate("testuser", "password"));
        assertEquals(GlobalErrorCode.LOGIN_FAILED, ex.getErrorCode());
        verify(loginAttemptTracker).recordIpFailure(anyString());
        verify(loginAttemptTracker).recordUsernameFailure("testuser");

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGIN_FAILED, event.eventType());
        assertEquals("ACCOUNT_DELETED", event.failureReason());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertFalse(event.success());
    }

    @Test
    void login_shouldThrowLoginFailed_whenUserDeleted() {
        testUser.setDeleted(true);
        when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(loginAttemptTracker.isIpLocked(anyString())).thenReturn(false);
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.authenticate("testuser", "password"));
        assertEquals(GlobalErrorCode.LOGIN_FAILED, ex.getErrorCode());
        verify(passwordEncoder).matches(eq("dummy"), anyString());
        verify(loginAttemptTracker).recordIpFailure(anyString());
        verify(loginAttemptTracker).recordUsernameFailure(anyString());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGIN_FAILED, event.eventType());
        assertEquals("ACCOUNT_DELETED", event.failureReason());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertFalse(event.success());
    }

    @Test
    void refreshToken_shouldSucceed() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(passwordChangeService.isChangeRequired(anyLong())).thenReturn(false);
        when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.of(1));
        when(jwtTokenProvider.getTokenVersionFromClaims(claims)).thenReturn(1);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString(), anyString(), anyInt())).thenReturn("new-refresh");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        TokenRefreshResponse response = authService.refreshToken("valid-token");

        assertNotNull(response);
        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.TOKEN_REFRESH_SUCCESS, event.eventType());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertTrue(event.success());
        assertNull(event.failureReason());
        assertEquals("jti-xxx", event.newJti());
    }

    @Test
    void refreshToken_shouldThrowOnInvalidToken() {
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken("invalid"));
        assertEquals(GlobalErrorCode.TOKEN_REFRESH_FAILED, ex.getErrorCode());
    }

    @Test
    void refreshToken_shouldThrowOnDisabledUser() {
        Claims claims = mock(Claims.class);
        testUser.setEnabled(false);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken("token"));
        assertEquals(GlobalErrorCode.TOKEN_REFRESH_FAILED, ex.getErrorCode());
        verify(loginAttemptTracker).recordIpFailure(anyString());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.TOKEN_REFRESH_REJECTED, event.eventType());
        assertEquals("DISABLED", event.failureReason());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertFalse(event.success());
    }

    @Test
    void refreshToken_shouldThrowOnDeletedUser() {
        Claims claims = mock(Claims.class);
        testUser.setDeleted(true);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken("token"));
        assertEquals(GlobalErrorCode.TOKEN_REFRESH_FAILED, ex.getErrorCode());
        verify(loginAttemptTracker).recordIpFailure(anyString());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.TOKEN_REFRESH_REJECTED, event.eventType());
        assertEquals("DELETED", event.failureReason());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertFalse(event.success());
    }

    @Test
    void refreshToken_shouldThrowOnUserNotFound() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken("token"));
        assertEquals(GlobalErrorCode.TOKEN_REFRESH_FAILED, ex.getErrorCode());
        verify(loginAttemptTracker).recordIpFailure(anyString());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.TOKEN_REFRESH_REJECTED, event.eventType());
        assertEquals("DELETED", event.failureReason());
        assertEquals(1L, event.userId());
        assertNull(event.username());
        assertFalse(event.success());
    }

    @Test
    void refreshToken_shouldThrowOnTokenVersionMismatch() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(passwordChangeService.isChangeRequired(anyLong())).thenReturn(false);
        when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.of(2));
        when(jwtTokenProvider.getTokenVersionFromClaims(claims)).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken("token"));
        assertEquals(GlobalErrorCode.TOKEN_REFRESH_FAILED, ex.getErrorCode());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.TOKEN_REFRESH_REJECTED, event.eventType());
        assertEquals("TOKEN_VERSION_MISMATCH", event.failureReason());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertFalse(event.success());
    }

    @Test
    void refreshToken_shouldThrowPasswordChangeRequired() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(passwordChangeService.isChangeRequired(anyLong())).thenReturn(true);

        assertThrows(PasswordChangeRequiredException.class,
                () -> authService.refreshToken("token"));
    }

    @Test
    void refreshToken_shouldThrowOnUsernameLocked() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken("token"));
        assertEquals(GlobalErrorCode.TOKEN_REFRESH_FAILED, ex.getErrorCode());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.TOKEN_REFRESH_REJECTED, event.eventType());
        assertEquals("LOCKED", event.failureReason());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertFalse(event.success());
    }

    @Test
    void refreshToken_shouldThrowOnTokenVersionNotFound() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(passwordChangeService.isChangeRequired(anyLong())).thenReturn(false);
        when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken("token"));
        assertEquals(GlobalErrorCode.TOKEN_REFRESH_FAILED, ex.getErrorCode());
    }

    @Test
    void refreshToken_shouldThrowOnSuspiciousRefresh() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(passwordChangeService.isChangeRequired(anyLong())).thenReturn(false);
        when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.of(1));
        when(jwtTokenProvider.getTokenVersionFromClaims(claims)).thenReturn(1);

        long now = System.currentTimeMillis();
        ConcurrentHashMap<Long, Deque<Long>> preFilled = new ConcurrentHashMap<>();
        ArrayDeque<Long> deque = new ArrayDeque<>();
        deque.addLast(now);
        deque.addLast(now);
        preFilled.put(1L, deque);
        ReflectionTestUtils.setField(authService, "refreshTimestamps", preFilled);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken("valid-token"));
        assertEquals(GlobalErrorCode.TOKEN_REFRESH_FAILED, ex.getErrorCode());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.TOKEN_REFRESH_REJECTED, event.eventType());
        assertEquals("SUSPICIOUS_REFRESH", event.failureReason());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertFalse(event.success());
    }

    @Test
    void refreshToken_shouldSucceedWhenBelowMaxCount() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(passwordChangeService.isChangeRequired(anyLong())).thenReturn(false);
        when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.of(1));
        when(jwtTokenProvider.getTokenVersionFromClaims(claims)).thenReturn(1);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString(), anyString(), anyInt())).thenReturn("new-refresh");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        long now = System.currentTimeMillis();
        ConcurrentHashMap<Long, Deque<Long>> preFilled = new ConcurrentHashMap<>();
        ArrayDeque<Long> deque = new ArrayDeque<>();
        deque.addLast(now);
        preFilled.put(1L, deque);
        ReflectionTestUtils.setField(authService, "refreshTimestamps", preFilled);

        TokenRefreshResponse response = authService.refreshToken("valid-token");
        assertNotNull(response);
        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
    }

    @Test
    void refreshToken_shouldSucceedAfterExpiredEntriesCleaned() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(passwordChangeService.isChangeRequired(anyLong())).thenReturn(false);
        when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.of(1));
        when(jwtTokenProvider.getTokenVersionFromClaims(claims)).thenReturn(1);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString(), anyString(), anyInt())).thenReturn("new-refresh");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        long expired = System.currentTimeMillis() - 10000L;
        ConcurrentHashMap<Long, Deque<Long>> preFilled = new ConcurrentHashMap<>();
        ArrayDeque<Long> deque = new ArrayDeque<>();
        deque.addLast(expired);
        deque.addLast(expired);
        preFilled.put(1L, deque);
        ReflectionTestUtils.setField(authService, "refreshTimestamps", preFilled);

        TokenRefreshResponse response = authService.refreshToken("valid-token");
        assertNotNull(response);
        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
    }

    @Test
    void refreshToken_shouldThrowOnSuspiciousRefreshWithExpiredAndFresh() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken(anyString(), eq("refresh"))).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-xxx");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(false);
        when(passwordChangeService.isChangeRequired(anyLong())).thenReturn(false);
        when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.of(1));
        when(jwtTokenProvider.getTokenVersionFromClaims(claims)).thenReturn(1);

        long now = System.currentTimeMillis();
        long expired = now - 10000L;
        ConcurrentHashMap<Long, Deque<Long>> preFilled = new ConcurrentHashMap<>();
        ArrayDeque<Long> deque = new ArrayDeque<>();
        deque.addLast(expired);
        deque.addLast(now);
        deque.addLast(now);
        preFilled.put(1L, deque);
        ReflectionTestUtils.setField(authService, "refreshTimestamps", preFilled);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken("valid-token"));
        assertEquals(GlobalErrorCode.TOKEN_REFRESH_FAILED, ex.getErrorCode());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.TOKEN_REFRESH_REJECTED, event.eventType());
        assertEquals("SUSPICIOUS_REFRESH", event.failureReason());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertFalse(event.success());
    }

    @Test
    void logout_shouldBlacklistToken() {
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 10000));
        when(claims.getSubject()).thenReturn("testuser");
        when(claims.get("jti", String.class)).thenReturn("jti-xxx");
        when(jwtTokenProvider.validateToken(anyString(), isNull())).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);

        authService.logout("valid-token");

        verify(tokenBlacklist).add(eq("jti-xxx"), anyLong());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGOUT, event.eventType());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertTrue(event.success());
        assertNull(event.failureReason());
        assertNull(event.refreshTokenMasked());
    }

    @Test
    void logout_shouldNotAuditWhenTokenNull() {
        authService.logout(null);

        verify(securityAuditLogger, never()).logAudit(any());
        verify(tokenBlacklist, never()).add(any(), anyLong());
    }

    @Test
    void logout_shouldNotAuditWhenTokenInvalid() {
        when(jwtTokenProvider.validateToken(anyString(), isNull())).thenReturn(null);

        authService.logout("invalid-token");

        verify(tokenBlacklist, never()).add(any(), anyLong());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGOUT, event.eventType());
        assertNull(event.userId());
        assertNull(event.username());
        assertTrue(event.success());
        assertNull(event.refreshTokenMasked());
    }

    @Test
    void logout_shouldRemoveRefreshTimestampsEntry() {
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 10000));
        when(claims.getSubject()).thenReturn("testuser");
        when(claims.get("jti", String.class)).thenReturn("jti-xxx");
        when(jwtTokenProvider.validateToken(anyString(), isNull())).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);

        long now = System.currentTimeMillis();
        ConcurrentHashMap<Long, Deque<Long>> preFilled = new ConcurrentHashMap<>();
        ArrayDeque<Long> deque = new ArrayDeque<>();
        deque.addLast(now);
        deque.addLast(now);
        preFilled.put(1L, deque);
        ReflectionTestUtils.setField(authService, "refreshTimestamps", preFilled);

        authService.logout("valid-token");

        verify(tokenBlacklist).add(eq("jti-xxx"), anyLong());

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Long, Deque<Long>> after =
                (ConcurrentHashMap<Long, Deque<Long>>) ReflectionTestUtils.getField(authService, "refreshTimestamps");
        assertNotNull(after);
        assertNull(after.get(1L));

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGOUT, event.eventType());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertTrue(event.success());
    }

    @Test
    void logout_shouldGetJtiFromClaims() {
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 10000));
        when(claims.getSubject()).thenReturn("testuser");
        when(claims.get("jti", String.class)).thenReturn("jti-xxx");
        when(jwtTokenProvider.validateToken(anyString(), isNull())).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);

        authService.logout("valid-token");

        verify(jwtTokenProvider, never()).getJtiFromToken(anyString());
        verify(tokenBlacklist).add(eq("jti-xxx"), anyLong());
    }

    @Test
    void logout_shouldAuditWithRefreshTokenMasked() {
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 10000));
        when(claims.getSubject()).thenReturn("testuser");
        when(claims.get("jti", String.class)).thenReturn("jti-xxx");
        when(jwtTokenProvider.validateToken(anyString(), isNull())).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);

        authService.logout("valid-token");

        verify(tokenBlacklist).add(eq("jti-xxx"), anyLong());

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.LOGOUT, event.eventType());
        assertEquals("refresh-***", event.refreshTokenMasked());
    }

    @Test
    void getCurrentUser_shouldSucceed() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userConverter.toUserInfoResponse(testUser)).thenReturn(
                new UserInfoResponse(1L, "testuser", "TestUser", null, null, "DOCTOR", null, java.util.Set.of()));

        UserInfoResponse response = authService.getCurrentUser(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    void getCurrentUser_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.getCurrentUser(999L));
        assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
        assertEquals("用户不存在", ex.getArgs()[0]);
    }

    @Test
    void getCurrentUser_shouldThrowWhenUserIdNull() {
        when(userRepository.findById(null)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.getCurrentUser(null));
        assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateProfile_shouldUpdateFieldsWithoutSave() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken(anyString(), isNull())).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userConverter.toUserInfoResponse(testUser)).thenReturn(
                new UserInfoResponse(1L, "testuser", "NewNick", "13900139000", "new@test.com", "DOCTOR", null, java.util.Set.of()));

        ProfileUpdateRequest request = new ProfileUpdateRequest("NewNick", "13900139000", "new@test.com");
        UserInfoResponse response = authService.updateProfile("token", request);

        assertNotNull(response);
        assertEquals("NewNick", response.realName());
        assertEquals("NewNick", testUser.getNickname());
        assertEquals("13900139000", testUser.getPhone());
        assertEquals("new@test.com", testUser.getEmail());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_shouldSucceed() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPass", "$2a$10$hashedpassword")).thenReturn(true);
        when(passwordPolicy.validate("newPass", "testuser")).thenReturn(null);
        when(passwordEncoder.encode("newPass")).thenReturn("$2a$10$newhash");

        try (MockedStatic<SecurityContextHolder> securityContext = mockStatic(SecurityContextHolder.class)) {
            authService.changePassword(1L, "oldPass", "newPass");
            securityContext.verify(SecurityContextHolder::clearContext);
        }

        assertEquals("$2a$10$newhash", testUser.getPassword());
        assertEquals(2, testUser.getTokenVersion());
        assertFalse(testUser.getPasswordChangeRequired());
        verify(passwordChangeService).clearChangeRequired(1L);

        ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(securityAuditLogger).logAudit(captor.capture());
        SecurityAuditEvent event = captor.getValue();
        assertEquals(SecurityAuditEventType.PASSWORD_CHANGED, event.eventType());
        assertEquals(1L, event.userId());
        assertEquals("testuser", event.username());
        assertTrue(event.success());
        assertNull(event.failureReason());
    }

    @Test
    void changePassword_shouldThrowOnOldPasswordMismatch() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOld", "$2a$10$hashedpassword")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.changePassword(1L, "wrongOld", "newPass"));
        assertEquals(GlobalErrorCode.PASSWORD_MISMATCH, ex.getErrorCode());
    }

    @Test
    void changePassword_shouldThrowOnPolicyViolation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPass", "$2a$10$hashedpassword")).thenReturn(true);
        when(passwordPolicy.validate("weak", "testuser")).thenReturn(GlobalErrorCode.PASSWORD_WEAK);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.changePassword(1L, "oldPass", "weak"));
        assertEquals(GlobalErrorCode.PASSWORD_WEAK, ex.getErrorCode());
    }
}
