package com.aimedical.modules.commonmodule.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginRequest")
class LoginRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("构造并访问字段")
    void shouldConstructAndAccessFields() {
        var request = new LoginRequest("testuser", "password123");
        assertEquals("testuser", request.username());
        assertEquals("password123", request.password());
    }

    @Test
    @DisplayName("正常字段通过校验")
    void shouldPassValidationWhenAllFieldsValid() {
        var request = new LoginRequest("testuser", "password123");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("username 为空时校验失败")
    void shouldFailWhenUsernameBlank() {
        var request = new LoginRequest("", "password123");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("password 为空时校验失败")
    void shouldFailWhenPasswordBlank() {
        var request = new LoginRequest("testuser", "");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("password 超过64字符时校验失败")
    void shouldFailWhenPasswordTooLong() {
        var request = new LoginRequest("testuser", "a".repeat(65));
        assertFalse(validator.validate(request).isEmpty());
    }
}
