package com.aimedical.modules.commonmodule.api;

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

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWT工具类
 *
 * <p>提供JWT令牌的生成、解析和验证功能。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public final class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /**
     * JWT签名密钥（Phase1使用固定密钥，生产环境应从配置读取）
     */
    private static final String SECRET_KEY = "AIMedicalSysPhase1JwtSecretKeyForDevelopment2026";

    /**
     * 令牌过期时间（秒）
     */
    private static final long EXPIRATION_TIME = 86400L;

    /**
     * 令牌类型
     */
    private static final String TOKEN_TYPE = "Bearer";

    private JwtUtil() {
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
    public static String generateToken(Long userId, String username, String role, String position) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        if (position != null) {
            claims.put("position", position);
        }

        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME * 1000);

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
     * @param token JWT令牌
     * @return Claims对象，包含令牌中的声明信息
     * @throws ExpiredJwtException      令牌已过期
     * @throws UnsupportedJwtException  不支持的令牌格式
     * @throws MalformedJwtException    令牌格式错误
     * @throws SignatureException       签名验证失败
     * @throws IllegalArgumentException 令牌为空或无效
     */
    public static Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
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
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
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
        return false;
    }

    /**
     * 从令牌中获取用户ID
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token JWT令牌
     * @return 用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 从令牌中获取角色类型
     *
     * @param token JWT令牌
     * @return 角色类型
     */
    public static String getRole(String token) {
        Claims claims = parseToken(token);
        Object role = claims.get("role");
        return role != null ? role.toString() : null;
    }

    /**
     * 从令牌中获取岗位类型
     *
     * @param token JWT令牌
     * @return 岗位类型，可能为空
     */
    public static String getPosition(String token) {
        Claims claims = parseToken(token);
        Object position = claims.get("position");
        return position != null ? position.toString() : null;
    }

    /**
     * 获取令牌过期时间（秒）
     *
     * @return 过期时间
     */
    public static Long getExpirationTime() {
        return EXPIRATION_TIME;
    }

    /**
     * 获取令牌类型
     *
     * @return 令牌类型
     */
    public static String getTokenType() {
        return TOKEN_TYPE;
    }
}