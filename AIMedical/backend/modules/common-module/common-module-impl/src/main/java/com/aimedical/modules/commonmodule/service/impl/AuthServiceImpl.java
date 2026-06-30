package com.aimedical.modules.commonmodule.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.commonmodule.api.AuthErrorCode;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.dto.*;
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
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.RoleRepository;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.aimedical.modules.commonmodule.api.UserType;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("phase1")
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final long REFRESH_WINDOW_MS = 5_000L;
    private static final int REFRESH_MAX_COUNT = 2;
    private static final int RATE_LIMIT_MAX = 5;
    private static final long RATE_LIMIT_WINDOW_MS = 10_000L;
    private static final int IP_LOCK_THRESHOLD = 20;
    private static final long IP_LOCK_DURATION_MS = 30 * 60 * 1000L;
    private static final int USERNAME_LOCK_THRESHOLD = 5;
    private static final long USERNAME_LOCK_DURATION_MS = 15 * 60 * 1000L;
    private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserConverter userConverter;
    private final PasswordPolicy passwordPolicy;
    private final PasswordChangeService passwordChangeService;
    private final RateLimitGuard rateLimitGuard;
    private final TokenBlacklist tokenBlacklist;
    private final LoginAttemptTracker loginAttemptTracker;
    private final SecurityAuditLogger securityAuditLogger;
    private final AuthServiceImpl self;

    private final ConcurrentHashMap<Long, Deque<Long>> refreshTimestamps = new ConcurrentHashMap<>();

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            UserConverter userConverter,
            PasswordPolicy passwordPolicy,
            PasswordChangeService passwordChangeService,
            RateLimitGuard rateLimitGuard,
            TokenBlacklist tokenBlacklist,
            LoginAttemptTracker loginAttemptTracker,
            SecurityAuditLogger securityAuditLogger,
            @Autowired @Lazy AuthServiceImpl self) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userConverter = userConverter;
        this.passwordPolicy = passwordPolicy;
        this.passwordChangeService = passwordChangeService;
        this.rateLimitGuard = rateLimitGuard;
        this.tokenBlacklist = tokenBlacklist;
        this.loginAttemptTracker = loginAttemptTracker;
        this.securityAuditLogger = securityAuditLogger;
        this.self = self;
    }

    @Override
    public TokenResponse authenticate(String username, String password) {
        String clientIp = getClientIp();

        if (!rateLimitGuard.tryAcquire(clientIp, RATE_LIMIT_MAX, RATE_LIMIT_WINDOW_MS)) {
            throw new BusinessException(GlobalErrorCode.RATE_LIMITED);
        }

        if (loginAttemptTracker.isIpLocked(clientIp)) {
            securityAuditLogger.logAudit(SecurityAuditEvent.now(
                    SecurityAuditEventType.LOGIN_FAILED, null, null, clientIp, false, "IP_LOCKED", null, null));
            throw new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "30分钟");
        }

        if (loginAttemptTracker.isUsernameLocked(username)) {
            securityAuditLogger.logAudit(SecurityAuditEvent.now(
                    SecurityAuditEventType.LOGIN_FAILED, null, null, clientIp, false, "USERNAME_LOCKED", null, null));
            throw new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "15分钟");
        }

        java.util.Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            passwordEncoder.matches("dummy", DUMMY_HASH);
            loginAttemptTracker.recordIpFailure(clientIp);
            securityAuditLogger.logAudit(SecurityAuditEvent.now(
                    SecurityAuditEventType.LOGIN_FAILED, null, null, clientIp, false, "USER_NOT_FOUND", null, null));
            throw new BusinessException(AuthErrorCode.AUTH_LOGIN_FAILED);
        }

        User user = userOpt.get();

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            passwordEncoder.matches("dummy", DUMMY_HASH);
            loginAttemptTracker.recordIpFailure(clientIp);
            loginAttemptTracker.recordUsernameFailure(user.getUsername());
            securityAuditLogger.logAudit(SecurityAuditEvent.now(
                    SecurityAuditEventType.LOGIN_FAILED, user.getId(), user.getUsername(), clientIp, false, "ACCOUNT_DISABLED", null, null));
            throw new BusinessException(AuthErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        if (Boolean.TRUE.equals(user.getDeleted())) {
            passwordEncoder.matches("dummy", DUMMY_HASH);
            loginAttemptTracker.recordIpFailure(clientIp);
            loginAttemptTracker.recordUsernameFailure(user.getUsername());
            securityAuditLogger.logAudit(SecurityAuditEvent.now(
                    SecurityAuditEventType.LOGIN_FAILED, user.getId(), user.getUsername(), clientIp, false, "ACCOUNT_DELETED", null, null));
            throw new BusinessException(AuthErrorCode.AUTH_LOGIN_FAILED);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginAttemptTracker.recordUsernameFailure(user.getUsername());
            securityAuditLogger.logAudit(SecurityAuditEvent.now(
                    SecurityAuditEventType.LOGIN_FAILED, user.getId(), user.getUsername(), clientIp, false, "BAD_CREDENTIALS", null, null));
            throw new BusinessException(AuthErrorCode.AUTH_LOGIN_FAILED);
        }

        loginAttemptTracker.clearIp(clientIp);
        loginAttemptTracker.clearUsername(user.getUsername());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(), user.getUserType().getCode());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getUsername(), user.getUserType().getCode(), user.getTokenVersion());

        securityAuditLogger.logAudit(SecurityAuditEvent.now(
                SecurityAuditEventType.LOGIN_SUCCESS, user.getId(), user.getUsername(), clientIp, true, null, null, null));

        return new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpirationMs());
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken == null) {
            return;
        }
        Claims claims = jwtTokenProvider.validateToken(accessToken, null);
        if (claims != null) {
            String jti = claims.get("jti", String.class);
            if (jti != null) {
                tokenBlacklist.add(jti, claims.getExpiration().getTime());
            }
            Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
            if (userId != null) refreshTimestamps.remove(userId);
        }
        log.info("用户登出成功");
    }

    // ── Patient-facing methods ──────────────────────────────────────────

    @Override
    public TokenResponse register(RegisterRequest request) {
        // DB 操作在独立事务中完成（self 代理调用，@Transactional 生效）
        User user = self.registerUser(request);
        // JWT 生成移出事务边界，避免 CPU 密集的签名操作拉长数据库事务持锁时间
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(), user.getUserType().getCode());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getUsername(), user.getUserType().getCode(), user.getTokenVersion());
        return new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpirationMs());
    }

    /**
     * 患者注册的数据库操作（用户+角色保存），独立事务保证原子性。
     * <p>需通过 self 代理调用以使 @Transactional 生效（同类自调用会绕过代理）。
     *
     * @param request 注册请求
     * @return 已持久化的用户实体
     */
    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.findByUsername(request.getPhone()).isPresent()) {
            throw new BusinessException(AuthErrorCode.AUTH_MOBILE_EXISTS);
        }
        User user = new User();
        user.setUsername(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getName());
        user.setGender(request.getGender());
        user.setAge(request.getAge());
        user.setUserType(UserType.PATIENT);
        user.setEnabled(true);
        userRepository.save(user);

        // Assign PATIENT role
        java.util.Optional<Role> patientRoleOpt = roleRepository.findByCode("PATIENT");
        if (patientRoleOpt.isEmpty()) {
            Role role = new Role();
            role.setCode("PATIENT");
            role.setName("患者");
            role.setEnabled(true);
            role = roleRepository.save(role);
            user.setRoles(new java.util.HashSet<>(java.util.Set.of(role)));
        } else {
            user.setRoles(new java.util.HashSet<>(java.util.Set.of(patientRoleOpt.get())));
        }
        userRepository.save(user);
        return user;
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        return authenticate(request.getPhone(), request.getPassword());
    }

    @Override
    public CurrentUserResponse getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID);
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID));
        CurrentUserResponse resp = new CurrentUserResponse();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setPhone(user.getPhone());
        resp.setGender(user.getGender());
        resp.setAge(user.getAge());
        resp.setUserType(user.getUserType().getCode());
        resp.setRoles(user.getRoles() != null
                ? user.getRoles().stream().map(r -> r.getCode()).toList()
                : java.util.Collections.emptyList());
        return resp;
    }

    @Override
    public TokenRefreshResponse refreshToken(String token) {
        Claims claims = jwtTokenProvider.validateToken(token, "refresh");
        if (claims == null) {
            throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
        }

        Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
        }

        String jti = jwtTokenProvider.getJtiFromToken(token);
        String clientIp = getClientIp();

        java.util.Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            loginAttemptTracker.recordIpFailure(clientIp);
            securityAuditLogger.logAudit(SecurityAuditEvent.now(
                    SecurityAuditEventType.TOKEN_REFRESH_REJECTED, userId, null, clientIp, false, "DELETED", null, null));
            throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
        }

        User user = userOpt.get();

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            loginAttemptTracker.recordIpFailure(clientIp);
            securityAuditLogger.logAudit(SecurityAuditEvent.now(
                    SecurityAuditEventType.TOKEN_REFRESH_REJECTED, userId, user.getUsername(), clientIp, false, "DISABLED", null, null));
            throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
        }

        if (Boolean.TRUE.equals(user.getDeleted())) {
            loginAttemptTracker.recordIpFailure(clientIp);
            securityAuditLogger.logAudit(SecurityAuditEvent.now(
                    SecurityAuditEventType.TOKEN_REFRESH_REJECTED, userId, user.getUsername(), clientIp, false, "DELETED", null, null));
            throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
        }

        if (loginAttemptTracker.isUsernameLocked(user.getUsername())) {
            securityAuditLogger.logAudit(SecurityAuditEvent.now(
                    SecurityAuditEventType.TOKEN_REFRESH_REJECTED, userId, user.getUsername(), clientIp, false, "LOCKED", null, null));
            throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
        }

        if (passwordChangeService.isChangeRequired(userId)) {
            throw new PasswordChangeRequiredException("需要修改密码");
        }

        java.util.Optional<Integer> tokenVersionOpt = userRepository.findTokenVersionById(userId);
        if (tokenVersionOpt.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
        }

        int dbTokenVersion = tokenVersionOpt.get();
        int claimTokenVersion = jwtTokenProvider.getTokenVersionFromClaims(claims);
        if (dbTokenVersion != claimTokenVersion) {
            securityAuditLogger.logAudit(SecurityAuditEvent.now(
                    SecurityAuditEventType.TOKEN_REFRESH_REJECTED, userId, user.getUsername(), clientIp, false, "TOKEN_VERSION_MISMATCH", null, null));
            throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
        }

        refreshTimestamps.compute(userId, (k, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
            }
            long now = System.currentTimeMillis();
            while (!deque.isEmpty() && deque.peekFirst() < now - REFRESH_WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= REFRESH_MAX_COUNT) {
                securityAuditLogger.logAudit(SecurityAuditEvent.now(
                        SecurityAuditEventType.TOKEN_REFRESH_REJECTED, userId, user.getUsername(), clientIp, false, "SUSPICIOUS_REFRESH", null, null));
                throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
            }
            deque.addLast(now);
            return deque;
        });

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(), user.getUserType().getCode());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getUsername(), user.getUserType().getCode(), dbTokenVersion);

        String newJti = jwtTokenProvider.getJtiFromToken(newAccessToken);

        securityAuditLogger.logAudit(SecurityAuditEvent.now(
                SecurityAuditEventType.TOKEN_REFRESH_SUCCESS, userId, user.getUsername(), clientIp, true, null, null, newJti));

        return new TokenRefreshResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirationMs());
    }

    @Override
    public UserInfoResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "用户不存在"));

        return userConverter.toUserInfoResponse(user);
    }

    @Override
    @Transactional
    public UserInfoResponse updateProfile(String token, ProfileUpdateRequest request) {
        Claims claims = jwtTokenProvider.validateToken(token, null);
        if (claims == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }

        Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));

        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }

        return userConverter.toUserInfoResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(GlobalErrorCode.PASSWORD_MISMATCH);
        }

        GlobalErrorCode policyError = passwordPolicy.validate(newPassword, user.getUsername());
        if (policyError != null) {
            throw new BusinessException(policyError);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setPasswordChangeRequired(false);
        passwordChangeService.clearChangeRequired(userId);

        securityAuditLogger.logAudit(SecurityAuditEvent.now(
                SecurityAuditEventType.PASSWORD_CHANGED, userId, user.getUsername(), getClientIp(), true, null, null, null));

        SecurityContextHolder.clearContext();

        log.info("用户密码修改成功，userId: {}", userId);
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.warn("获取客户端 IP 失败: {}", e.getMessage());
            return "unknown";
        }
    }
}
