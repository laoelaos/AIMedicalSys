package com.aimedical.modules.commonmodule.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RefreshTokenRequest")
class RefreshTokenRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("构造并访问 refreshToken 字段")
    void shouldConstructAndAccessField() {
        var request = new RefreshTokenRequest("eyJhbGciOiJIUzI1NiJ9.token");
        assertEquals("eyJhbGciOiJIUzI1NiJ9.token", request.refreshToken());
    }

    @Test
    @DisplayName("refreshToken 为空时校验失败")
    void shouldFailWhenRefreshTokenBlank() {
        var request = new RefreshTokenRequest("");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("refreshToken 非空时通过校验")
    void shouldPassWhenRefreshTokenPresent() {
        var request = new RefreshTokenRequest("valid-token");
        assertTrue(validator.validate(request).isEmpty());
    }
}
