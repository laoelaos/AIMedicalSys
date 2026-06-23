package com.aimedical.modules.commonmodule.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.commonmodule.api.JwtUtil;
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
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Optional<User> userOptional = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(request.getUsername()))
                .findFirst();

        if (userOptional.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.NOT_FOUND, "用户不存在");
        }

        User user = userOptional.get();

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(GlobalErrorCode.NOT_FOUND, "用户已被禁用");
        }

        // Phase1: 使用明文密码比对（生产环境应使用BCrypt）
        if (!request.getPassword().equals(user.getPassword())) {
            throw new BusinessException(GlobalErrorCode.NOT_FOUND, "密码错误");
        }

        String position = null;
        if (user.getUserType() == UserType.DOCTOR && user.getPosts() != null && !user.getPosts().isEmpty()) {
            Post post = user.getPosts().iterator().next();
            position = post.getCode();
        }

        String token = JwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getUserType().getCode(),
                position
        );

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType(JwtUtil.getTokenType());
        response.setExpiresIn(JwtUtil.getExpirationTime());
        response.setUser(buildUserInfoResponse(user));

        return response;
    }

    @Override
    public void logout(String token) {
        // Phase1: JWT无状态，登出只需客户端清除令牌
        // 生产环境可实现令牌黑名单
    }

    @Override
    public LoginResponse refreshToken(String token) {
        if (!JwtUtil.validateToken(token)) {
            throw new BusinessException(GlobalErrorCode.NOT_FOUND, "令牌无效");
        }

        Long userId = JwtUtil.getUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "用户不存在"));

        String position = JwtUtil.getPosition(token);

        String newToken = JwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getUserType().getCode(),
                position
        );

        LoginResponse response = new LoginResponse();
        response.setToken(newToken);
        response.setTokenType(JwtUtil.getTokenType());
        response.setExpiresIn(JwtUtil.getExpirationTime());
        response.setUser(buildUserInfoResponse(user));

        return response;
    }

    @Override
    public UserInfoResponse getCurrentUser(String token) {
        if (!JwtUtil.validateToken(token)) {
            throw new BusinessException(GlobalErrorCode.NOT_FOUND, "令牌无效");
        }

        Long userId = JwtUtil.getUserId(token);
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