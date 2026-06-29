package com.aimedical.modules.commonmodule.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtConfig单元测试
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@DisplayName("JwtConfig测试")
class JwtConfigTest {

    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
    }

    @Nested
    @DisplayName("默认值")
    class DefaultValueTests {

        @Test
        @DisplayName("accessTokenExpiration 默认值为 900L")
        void shouldDefaultAccessTokenExpirationTo900() {
            assertEquals(900L, jwtConfig.getAccessTokenExpiration());
        }

        @Test
        @DisplayName("refreshTokenExpiration 默认值为 604800L")
        void shouldDefaultRefreshTokenExpirationTo604800() {
            assertEquals(604800L, jwtConfig.getRefreshTokenExpiration());
        }

        @Test
        @DisplayName("tokenType 默认值为 Bearer")
        void shouldDefaultTokenTypeToBearer() {
            assertEquals("Bearer", jwtConfig.getTokenType());
        }
    }

    @Nested
    @DisplayName("getter/setter")
    class GetterSetterTests {

        @Test
        @DisplayName("设置和获取 accessTokenExpiration 成功")
        void shouldSetAndGetAccessTokenExpiration() {
            jwtConfig.setAccessTokenExpiration(1800L);
            assertEquals(1800L, jwtConfig.getAccessTokenExpiration());
        }

        @Test
        @DisplayName("设置和获取 refreshTokenExpiration 成功")
        void shouldSetAndGetRefreshTokenExpiration() {
            jwtConfig.setRefreshTokenExpiration(1209600L);
            assertEquals(1209600L, jwtConfig.getRefreshTokenExpiration());
        }

        @Test
        @DisplayName("设置和获取 secret 成功")
        void shouldSetAndGetSecret() {
            jwtConfig.setSecret("this-is-a-test-secret-key-for-unit-testing");
            assertEquals("this-is-a-test-secret-key-for-unit-testing", jwtConfig.getSecret());
        }

        @Test
        @DisplayName("设置和获取 tokenType 成功")
        void shouldSetAndGetTokenType() {
            jwtConfig.setTokenType("Custom");
            assertEquals("Custom", jwtConfig.getTokenType());
        }
    }

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("密钥为null抛出IllegalStateException")
        void shouldThrowExceptionWhenSecretIsNull() {
            jwtConfig.setSecret(null);
            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtConfig::validate);
            assertTrue(ex.getMessage().contains("JWT密钥未配置"));
        }

        @Test
        @DisplayName("密钥为空字符串抛出IllegalStateException")
        void shouldThrowExceptionWhenSecretIsEmpty() {
            jwtConfig.setSecret("");
            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtConfig::validate);
            assertTrue(ex.getMessage().contains("JWT密钥未配置"));
        }

        @Test
        @DisplayName("密钥不是合法Base64字符串抛出IllegalStateException")
        void shouldThrowWhenSecretIsInvalidBase64() {
            jwtConfig.setSecret("!!!invalid!!!base64");
            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtConfig::validate);
            assertTrue(ex.getMessage().contains("not a valid Base64 string"));
        }

        @Test
        @DisplayName("解码后密钥长度小于32字节抛出IllegalStateException")
        void shouldThrowWhenDecodedKeyTooShort() {
            jwtConfig.setSecret("dGVzdA==");
            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtConfig::validate);
            assertTrue(ex.getMessage().contains("至少32字节"));
        }

        @Test
        @DisplayName("有效密钥不抛出异常")
        void shouldPassWithValidLongSecret() {
            jwtConfig.setSecret("AIMedicalSysJwtSecretKey2026Phase1DevelopmentTestSecretKey");
            assertDoesNotThrow(jwtConfig::validate);
        }
    }
}
