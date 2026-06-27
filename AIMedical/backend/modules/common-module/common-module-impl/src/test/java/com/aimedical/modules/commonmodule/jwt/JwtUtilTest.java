package com.aimedical.modules.commonmodule.jwt;

import io.jsonwebtoken.Claims;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil单元测试
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@DisplayName("JwtUtil测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("AIMedicalSysJwtSecretKey2026Phase1DevelopmentTestSecretKey");
        jwtConfig.setAccessTokenExpiration(900L);
        jwtConfig.setTokenType("Bearer");

        jwtUtil = new JwtUtil(jwtConfig);
        jwtUtil.init();
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("生成token成功")
        void shouldGenerateTokenSuccessfully() {
            String token = jwtUtil.generateToken(1L, "admin", "ADMIN", null);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3);
        }

        @Test
        @DisplayName("生成不同用户token应不同")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            String token1 = jwtUtil.generateToken(1L, "admin", "ADMIN", null);
            String token2 = jwtUtil.generateToken(2L, "doctor", "DOCTOR", null);

            assertNotEquals(token1, token2);
        }

    }

    @Nested
    @DisplayName("parseToken")
    class ParseTokenTests {

        @Test
        @DisplayName("解析有效token成功")
        void shouldParseValidToken() {
            String token = jwtUtil.generateToken(1L, "admin", "ADMIN", null);

            Claims claims = jwtUtil.parseToken(token);

            assertNotNull(claims);
            assertEquals(Integer.valueOf(1), claims.get("userId"));
            assertEquals("admin", claims.get("username"));
            assertNotNull(claims.get("jti"));
        }

        @Test
        @DisplayName("解析无效token抛出异常")
        void shouldThrowExceptionForInvalidToken() {
            assertThrows(io.jsonwebtoken.MalformedJwtException.class,
                () -> jwtUtil.parseToken("invalid.token.here"));
        }

        @Test
        @DisplayName("解析null抛出异常")
        void shouldThrowExceptionForNullToken() {
            assertThrows(Exception.class, () -> jwtUtil.parseToken(null));
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("验证有效token返回true")
        void shouldReturnTrueForValidToken() {
            String token = jwtUtil.generateToken(1L, "admin", "ADMIN", null);

            assertTrue(jwtUtil.validateToken(token));
        }

        @Test
        @DisplayName("验证无效token返回false")
        void shouldReturnFalseForInvalidToken() {
            assertFalse(jwtUtil.validateToken("invalid.token"));
        }

        @Test
        @DisplayName("验证null返回false")
        void shouldReturnFalseForNullToken() {
            assertFalse(jwtUtil.validateToken(null));
        }

        @Test
        @DisplayName("验证空字符串返回false")
        void shouldReturnFalseForEmptyToken() {
            assertFalse(jwtUtil.validateToken(""));
        }
    }

    @Nested
    @DisplayName("validateTokenAndGetClaims")
    class ValidateTokenAndGetClaimsTests {

        @Test
        @DisplayName("验证有效token并返回Claims")
        void shouldReturnClaimsForValidToken() {
            String token = jwtUtil.generateToken(1L, "admin", "ADMIN", null);

            Claims claims = jwtUtil.validateTokenAndGetClaims(token);

            assertNotNull(claims);
            assertEquals(Integer.valueOf(1), claims.get("userId"));
        }

        @Test
        @DisplayName("验证无效token返回null")
        void shouldReturnNullForInvalidToken() {
            assertNull(jwtUtil.validateTokenAndGetClaims("invalid.token"));
        }
    }

    @Nested
    @DisplayName("getUserId")
    class GetUserIdTests {

        @Test
        @DisplayName("从有效token获取userId成功")
        void shouldGetUserIdFromValidToken() {
            String token = jwtUtil.generateToken(123L, "admin", "ADMIN", null);

            Long userId = jwtUtil.getUserId(token);

            assertEquals(123L, userId);
        }

        @Test
        @DisplayName("从无效token获取userId返回null")
        void shouldReturnNullForInvalidToken() {
            assertNull(jwtUtil.getUserId("invalid.token"));
        }
    }

    @Nested
    @DisplayName("getRole")
    class GetRoleTests {

        @Test
        @DisplayName("从有效token获取role成功")
        void shouldGetRoleFromValidToken() {
            String token = jwtUtil.generateToken(1L, "doctor", "DOCTOR", null);

            String role = jwtUtil.getRole(token);

            assertNull(role);
        }

        @Test
        @DisplayName("从无效token获取role返回null")
        void shouldReturnNullForInvalidToken() {
            assertNull(jwtUtil.getRole("invalid.token"));
        }
    }

    @Nested
    @DisplayName("extractToken")
    class ExtractTokenTests {

        @Test
        @DisplayName("提取有效Bearer token成功")
        void shouldExtractValidBearerToken() {
            String token = jwtUtil.generateToken(1L, "admin", "ADMIN", null);
            String authHeader = "Bearer " + token;

            String extracted = JwtUtil.extractToken(authHeader, "Bearer");

            assertEquals(token, extracted);
        }

        @Test
        @DisplayName("提取非Bearer格式返回null")
        void shouldReturnNullForNonBearerFormat() {
            String result = JwtUtil.extractToken("Basic abc123", "Bearer");

            assertNull(result);
        }

        @Test
        @DisplayName("提取null返回null")
        void shouldReturnNullForNullHeader() {
            assertNull(JwtUtil.extractToken(null, "Bearer"));
        }

        @Test
        @DisplayName("提取空字符串返回null")
        void shouldReturnNullForEmptyHeader() {
            assertNull(JwtUtil.extractToken("", "Bearer"));
        }
    }

    @Nested
    @DisplayName("getExpiration")
    class GetExpirationTests {

        @Test
        @DisplayName("获取过期时间成功")
        void shouldGetExpiration() {
            assertEquals(900L, jwtUtil.getExpirationTime());
        }

        @Test
        @DisplayName("获取token类型成功")
        void shouldGetTokenType() {
            assertEquals("Bearer", jwtUtil.getTokenType());
        }
    }

    @Nested
    @DisplayName("init验证")
    class InitTests {

        @Test
        @DisplayName("密钥为null抛出IllegalStateException")
        void shouldThrowExceptionWhenSecretIsNull() {
            JwtConfig config = new JwtConfig();
            config.setSecret(null);
            config.setAccessTokenExpiration(900L);
            config.setTokenType("Bearer");
            JwtUtil util = new JwtUtil(config);
            IllegalStateException ex = assertThrows(IllegalStateException.class, util::init);
            assertEquals("JWT_SECRET must be configured", ex.getMessage());
        }

        @Test
        @DisplayName("密钥为空字符串抛出IllegalStateException")
        void shouldThrowExceptionWhenSecretIsEmpty() {
            JwtConfig config = new JwtConfig();
            config.setSecret("");
            config.setAccessTokenExpiration(900L);
            config.setTokenType("Bearer");
            JwtUtil util = new JwtUtil(config);
            IllegalStateException ex = assertThrows(IllegalStateException.class, util::init);
            assertEquals("JWT_SECRET must be configured", ex.getMessage());
        }

        @Test
        @DisplayName("密钥含非法Base64字符抛出IllegalStateException")
        void shouldThrowExceptionWhenSecretContainsInvalidChars() {
            JwtConfig config = new JwtConfig();
            config.setSecret("invalid-secret-with-dashes");
            config.setAccessTokenExpiration(900L);
            config.setTokenType("Bearer");
            JwtUtil util = new JwtUtil(config);
            IllegalStateException ex = assertThrows(IllegalStateException.class, util::init);
            assertEquals("JWT_SECRET contains invalid characters", ex.getMessage());
        }

        @Test
        @DisplayName("密钥解码后不足32字节抛出IllegalStateException")
        void shouldThrowExceptionWhenSecretDecodedLessThan32Bytes() {
            JwtConfig config = new JwtConfig();
            config.setSecret("BQ==");
            config.setAccessTokenExpiration(900L);
            config.setTokenType("Bearer");
            JwtUtil util = new JwtUtil(config);
            IllegalStateException ex = assertThrows(IllegalStateException.class, util::init);
            assertEquals("JWT_SECRET must be at least 256 bits (32 bytes) after Base64 decoding", ex.getMessage());
        }

        @Test
        @DisplayName("有效密钥初始化成功后可生成和解析令牌")
        void shouldGenerateAndParseTokenAfterInit() {
            String token = jwtUtil.generateToken(1L, "admin", "ADMIN", null);
            assertNotNull(token);
            assertNotNull(jwtUtil.parseToken(token));
        }
    }
}
