package com.aimedical.modules.commonmodule.dto.response;

/**
 * 登录响应DTO
 *
 * <p>用于返回登录成功后的JWT令牌和用户基本信息。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class LoginResponse {

    /**
     * JWT令牌
     */
    private String token;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户基本信息
     */
    private UserInfoResponse user;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserInfoResponse getUser() {
        return user;
    }

    public void setUser(UserInfoResponse user) {
        this.user = user;
    }
}