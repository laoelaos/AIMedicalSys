package com.aimedical.modules.commonmodule.api;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置类
 *
 * <p>从配置文件读取JWT相关配置。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT签名密钥
     */
    private String secret = "AIMedicalSysJwtSecretKey2026Phase1Development";

    /**
     * 令牌过期时间（秒）
     */
    private long expiration = 86400L;

    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}