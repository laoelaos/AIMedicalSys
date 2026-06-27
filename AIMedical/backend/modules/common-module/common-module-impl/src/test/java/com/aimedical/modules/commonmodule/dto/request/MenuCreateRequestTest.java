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

@DisplayName("MenuCreateRequest")
class MenuCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("构造并访问字段")
    void shouldConstructAndAccessFields() {
        var request = new MenuCreateRequest("菜单", "menu:test", null, "/path", "Component", "icon", 1, true);
        assertEquals("菜单", request.name());
        assertEquals("menu:test", request.permission());
        assertNull(request.parentId());
        assertEquals("/path", request.path());
        assertEquals("Component", request.component());
        assertEquals("icon", request.icon());
        assertEquals(1, request.sort());
        assertTrue(request.visible());
    }

    @Test
    @DisplayName("所有必填字段合法通过校验")
    void shouldPassValidationWhenRequiredFieldsValid() {
        var request = new MenuCreateRequest("菜单", "menu:test", null, null, null, null, null, true);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("name 为空时校验失败")
    void shouldFailWhenNameBlank() {
        var request = new MenuCreateRequest("", "menu:test", null, null, null, null, null, true);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("permission 为空时校验失败")
    void shouldFailWhenPermissionBlank() {
        var request = new MenuCreateRequest("菜单", "", null, null, null, null, null, true);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("visible 为 null 时校验失败")
    void shouldFailWhenVisibleNull() {
        var request = new MenuCreateRequest("菜单", "menu:test", null, null, null, null, null, null);
        Set<ConstraintViolation<MenuCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("visible")));
    }

    @Test
    @DisplayName("可选字段 parentId/path/component/icon/sort 可为 null")
    void shouldAcceptNullOptionalFields() {
        var request = new MenuCreateRequest("菜单", "menu:test", null, null, null, null, null, true);
        assertNull(request.parentId());
        assertNull(request.path());
        assertNull(request.component());
        assertNull(request.icon());
        assertNull(request.sort());
    }
}
