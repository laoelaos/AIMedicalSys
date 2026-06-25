package com.aimedical.modules.commonmodule.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JWT工具类
 *
 * <p>提供JWT令牌的生成、解析和验证功能。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final JwtConfig jwtConfig;

    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * 生成JWT令牌
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色类型
     * @param position 岗位类型（可为空）
     * @return JWT令牌字符串
     */
    public String generateToken(Long userId, String username, String role, String position) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        if (position != null) {
            claims.put("position", position);
        }

        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpiration() * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * 解析JWT令牌
     *
     * <p>注意：本方法不会返回 null，解析失败时会抛出异常。
     *
     * @param token JWT令牌
     * @return Claims对象，包含令牌中的声明信息
     * @throws ExpiredJwtException      令牌已过期
     * @throws UnsupportedJwtException  不支持的令牌格式
     * @throws MalformedJwtException    令牌格式错误
     * @throws SignatureException       签名验证失败
     * @throws IllegalArgumentException 令牌为空或无效
     */
    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证JWT令牌是否有效
     *
     * @param token JWT令牌
     * @return true表示令牌有效，false表示令牌无效
     */
    public boolean validateToken(String token) {
        return validateTokenAndGetClaims(token) != null;
    }

    /**
     * 验证JWT令牌并返回Claims对象
     *
     * <p>统一验证和解析逻辑，避免重复解析JWT。
     *
     * @param token JWT令牌
     * @return Claims对象，验证失败返回null
     */
    public Claims validateTokenAndGetClaims(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return parseToken(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT令牌已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT令牌格式: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT令牌格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT签名验证失败: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT令牌为空或无效: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 安全地从令牌中提取声明值
     *
     * <p>统一处理解析异常，避免每个 getter 方法重复 try-catch 样板代码。
     *
     * @param token JWT令牌
     * @param extractor 从Claims中提取值的函数
     * @param <T> 返回值类型
     * @return 提取的值，解析失败返回null
     */
    private <T> T getClaimFromToken(String token, Function<Claims, T> extractor) {
        Claims claims = validateTokenAndGetClaims(token);
        if (claims == null) {
            return null;
        }
        return extractor.apply(claims);
    }

    /**
     * 从令牌中获取用户ID
     *
     * @param token JWT令牌
     * @return 用户ID，解析失败返回null
     */
    public Long getUserId(String token) {
        return getClaimFromToken(token, claims -> {
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            } else if (userId instanceof Long) {
                return (Long) userId;
            }
            return null;
        });
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token JWT令牌
     * @return 用户名，解析失败返回null
     */
    public String getUsername(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从令牌中获取角色类型
     *
     * @param token JWT令牌
     * @return 角色类型，解析失败返回null
     */
    public String getRole(String token) {
        return getClaimFromToken(token, claims -> {
            Object role = claims.get("role");
            return role != null ? role.toString() : null;
        });
    }

    /**
     * 从令牌中获取岗位类型
     *
     * @param token JWT令牌
     * @return 岗位类型，解析失败返回null
     */
    public String getPosition(String token) {
        return getClaimFromToken(token, claims -> {
            Object position = claims.get("position");
            return position != null ? position.toString() : null;
        });
    }

    /**
     * 获取令牌过期时间（秒）
     *
     * @return 过期时间
     */
    public Long getExpirationTime() {
        return jwtConfig.getExpiration();
    }

    /**
     * 获取令牌类型
     *
     * @return 令牌类型
     */
    public String getTokenType() {
        return jwtConfig.getTokenType();
    }

    /**
     * 从Authorization头中提取JWT令牌（静态方法）
     *
     * <p>统一提取逻辑：只有当header以指定tokenType + 空格开头时才提取token，
     * 否则返回null。这确保了只有正确格式的Bearer token才会被处理。
     *
     * @param authHeader Authorization请求头
     * @param tokenType 令牌类型（如"Bearer"）
     * @return JWT令牌，如果格式不正确则返回null
     */
    public static String extractToken(String authHeader, String tokenType) {
        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }
        if (authHeader.startsWith(tokenType + " ")) {
            return authHeader.substring(tokenType.length() + 1);
        }
        return null;
    }
}
