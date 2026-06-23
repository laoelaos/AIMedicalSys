package com.aimedical.modules.commonmodule.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.commonmodule.jwt.JwtUtil;
import com.aimedical.modules.commonmodule.api.UserType;
import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.response.LoginResponse;
import com.aimedical.modules.commonmodule.dto.response.UserInfoResponse;
import com.aimedical.modules.commonmodule.permission.Function;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.aimedical.modules.commonmodule.service.AuthService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 *
 * <p>实现用户认证相关的业务逻辑。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername());

        if (user == null) {
            log.warn("登录失败：用户不存在，用户名: {}", request.getUsername());
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            log.warn("登录失败：用户已被禁用，用户名: {}", request.getUsername());
            throw new BusinessException(GlobalErrorCode.FORBIDDEN, "用户已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败：密码错误，用户名: {}", request.getUsername());
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        String position = null;
        if (user.getUserType() == UserType.DOCTOR && user.getPosts() != null && !user.getPosts().isEmpty()) {
            Post post = user.getPosts().iterator().next();
            position = post.getCode();
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getUserType().getCode(),
                position
        );

        log.info("用户登录成功，用户名: {}", request.getUsername());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType(jwtUtil.getTokenType());
        response.setExpiresIn(jwtUtil.getExpirationTime());
        response.setUser(buildUserInfoResponse(user));

        return response;
    }

    @Override
    public void logout(String token) {
        // Phase1: JWT无状态，登出只需客户端清除令牌
        // 生产环境可实现令牌黑名单
        log.info("用户登出");
    }

    @Override
    public LoginResponse refreshToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "令牌无效");
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "令牌无效");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "用户不存在"));

        String position = jwtUtil.getPosition(token);

        String newToken = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getUserType().getCode(),
                position
        );

        LoginResponse response = new LoginResponse();
        response.setToken(newToken);
        response.setTokenType(jwtUtil.getTokenType());
        response.setExpiresIn(jwtUtil.getExpirationTime());
        response.setUser(buildUserInfoResponse(user));

        return response;
    }

    @Override
    public UserInfoResponse getCurrentUser(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "令牌无效");
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "令牌无效");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "用户不存在"));

        return buildUserInfoResponse(user);
    }

    /**
     * 构建用户信息响应
     *
     * @param user 用户实体
     * @return 用户信息响应
     */
    private UserInfoResponse buildUserInfoResponse(User user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getNickname());
        response.setRole(user.getUserType().getCode());

        if (user.getUserType() == UserType.DOCTOR && user.getPosts() != null && !user.getPosts().isEmpty()) {
            Post post = user.getPosts().iterator().next();
            response.setPosition(post.getCode());
        }

        List<String> permissions = new ArrayList<>();
        if (user.getPosts() != null) {
            Set<Function> functions = new HashSet<>();
            for (Post post : user.getPosts()) {
                if (post.getFunctions() != null) {
                    functions.addAll(post.getFunctions());
                }
            }
            for (Function function : functions) {
                permissions.add(function.getCode());
            }
        }
        response.setPermissions(permissions);

        return response;
    }
}