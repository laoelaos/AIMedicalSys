package com.aimedical.modules.commonmodule.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordChangeRequest")
class PasswordChangeRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("构造并访问字段")
    void shouldConstructAndAccessFields() {
        var request = new PasswordChangeRequest("oldPass123", "newPass123");
        assertEquals("oldPass123", request.oldPassword());
        assertEquals("newPass123", request.newPassword());
    }

    @Test
    @DisplayName("合法字段通过校验")
    void shouldPassWhenAllFieldsValid() {
        var request = new PasswordChangeRequest("oldPass123", "newPass123");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("oldPassword 为空时校验失败")
    void shouldFailWhenOldPasswordBlank() {
        var request = new PasswordChangeRequest("", "newPass123");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("newPassword 为空时校验失败")
    void shouldFailWhenNewPasswordBlank() {
        var request = new PasswordChangeRequest("oldPass123", "");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("newPassword 少于8字符时校验失败")
    void shouldFailWhenNewPasswordTooShort() {
        var request = new PasswordChangeRequest("oldPass123", "short12");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("newPassword 超过64字符时校验失败")
    void shouldFailWhenNewPasswordTooLong() {
        var request = new PasswordChangeRequest("oldPass123", "a".repeat(65));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("oldPassword 超过128字符时校验失败")
    void shouldFailWhenOldPasswordTooLong() {
        var request = new PasswordChangeRequest("a".repeat(129), "newPass123");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("oldPassword 为 1 个字符时校验通过（仅 upper bound 128）")
    void shouldPassWhenOldPasswordIsOneChar() {
        var request = new PasswordChangeRequest("a", "newPass123");
        assertTrue(validator.validate(request).isEmpty());
    }
}
