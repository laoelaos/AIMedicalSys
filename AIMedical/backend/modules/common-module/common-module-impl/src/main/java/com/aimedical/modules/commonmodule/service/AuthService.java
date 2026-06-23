package com.aimedical.modules.commonmodule.service;

import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.response.LoginResponse;
import com.aimedical.modules.commonmodule.dto.response.UserInfoResponse;

/**
 * 认证服务接口
 *
 * <p>提供用户认证相关的业务操作，包括登录、登出、刷新令牌和获取用户信息。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应，包含JWT令牌和用户信息
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户登出
     *
     * @param token JWT令牌
     */
    void logout(String token);

    /**
     * 刷新令牌
     *
     * @param token 当前JWT令牌
     * @return 新的登录响应
     */
    LoginResponse refreshToken(String token);

    /**
     * 获取当前用户信息
     *
     * @param token JWT令牌
     * @return 用户信息响应
     */
    UserInfoResponse getCurrentUser(String token);
}