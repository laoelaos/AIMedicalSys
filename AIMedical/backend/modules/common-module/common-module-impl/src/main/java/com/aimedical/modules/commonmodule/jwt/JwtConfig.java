package com.aimedical.modules.commonmodule.jwt;

import jakarta.annotation.PostConstruct;

import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * JWT配置类
 *
 * <p>从配置文件读取JWT相关配置。生产环境必须通过环境变量或配置文件提供JWT密钥。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    /**
     * JWT签名密钥（生产环境必须配置）
     */
    private String secret;

    /**
     * Access Token 过期时间（秒），默认15分钟
     */
    private long accessTokenExpiration = 900L;

    /**
     * Refresh Token 过期时间（秒），默认7天
     */
    private long refreshTokenExpiration = 604800L;

    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";

    /**
     * 验证配置完整性
     */
    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                "JWT密钥未配置！请通过环境变量 JWT_SECRET 或配置项 jwt.secret 提供密钥。"
            );
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getUrlDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT secret is not a valid Base64 string: " + e.getMessage());
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT密钥解码后字节长度必须至少32字节（256位）。当前解码后长度: " + keyBytes.length
            );
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(long accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(long refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
