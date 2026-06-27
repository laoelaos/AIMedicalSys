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

@DisplayName("ProfileUpdateRequest")
class ProfileUpdateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("构造并访问字段")
    void shouldConstructAndAccessFields() {
        var request = new ProfileUpdateRequest("昵称", "13800138000", "test@example.com");
        assertEquals("昵称", request.nickname());
        assertEquals("13800138000", request.phone());
        assertEquals("test@example.com", request.email());
    }

    @Test
    @DisplayName("所有字段合法通过校验")
    void shouldPassValidationWhenAllFieldsValid() {
        var request = new ProfileUpdateRequest("昵称", "13800138000", "test@example.com");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("nickname 为空时校验失败")
    void shouldFailWhenNicknameBlank() {
        var request = new ProfileUpdateRequest("", "13800138000", "test@example.com");
        Set<ConstraintViolation<ProfileUpdateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nickname")));
    }

    @Test
    @DisplayName("nickname 超过50字符时校验失败")
    void shouldFailWhenNicknameTooLong() {
        var request = new ProfileUpdateRequest("a".repeat(51), null, null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("phone 格式错误时校验失败")
    void shouldFailWhenPhoneInvalid() {
        var request = new ProfileUpdateRequest("昵称", "12345678901", null);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("phone 可选为空")
    void shouldPassWhenPhoneNull() {
        var request = new ProfileUpdateRequest("昵称", null, null);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("email 格式错误时校验失败")
    void shouldFailWhenEmailInvalid() {
        var request = new ProfileUpdateRequest("昵称", null, "not-an-email");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("email 可选为空")
    void shouldPassWhenEmailNull() {
        var request = new ProfileUpdateRequest("昵称", null, null);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("email 超过100字符时校验失败")
    void shouldFailWhenEmailTooLong() {
        var request = new ProfileUpdateRequest("昵称", null, "a".repeat(90) + "@example.com");
        assertFalse(validator.validate(request).isEmpty());
    }
}
