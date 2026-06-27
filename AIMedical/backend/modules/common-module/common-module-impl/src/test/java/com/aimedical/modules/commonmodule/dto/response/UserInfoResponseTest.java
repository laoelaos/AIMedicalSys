package com.aimedical.modules.commonmodule.dto.response;

import com.aimedical.modules.commonmodule.auth.UserInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserInfoResponse")
class UserInfoResponseTest {

    @Test
    @DisplayName("构造并访问所有字段")
    void shouldConstructAndAccessFields() {
        var permissions = Set.of("menu:dashboard", "menu:users");
        var response = new UserInfoResponse(1L, "testuser", "测试用户",
                "13800138000", "test@test.com", "DOCTOR", "DOC_GENERAL", permissions);

        assertEquals(1L, response.id());
        assertEquals("testuser", response.username());
        assertEquals("测试用户", response.realName());
        assertEquals("13800138000", response.phone());
        assertEquals("test@test.com", response.email());
        assertEquals("DOCTOR", response.role());
        assertEquals("DOC_GENERAL", response.position());
        assertEquals(permissions, response.permissions());
    }

    @Test
    @DisplayName("permissions 为 Set<String> 类型保证去重")
    void shouldHaveSetPermissions() {
        var permissions = new HashSet<>(Arrays.asList("perm1", "perm2", "perm1"));
        assertEquals(2, permissions.size());
        var response = new UserInfoResponse(1L, "u", "n", null, null, "r", null, permissions);
        assertInstanceOf(Set.class, response.permissions());
        assertEquals(2, response.permissions().size());
    }

    @Test
    @DisplayName("position 可为 null")
    void shouldAcceptNullPosition() {
        var response = new UserInfoResponse(1L, "u", "n", null, null, "r", null, Set.of());
        assertNull(response.position());
    }

    @Test
    @DisplayName("phone 可为 null")
    void shouldAcceptNullPhone() {
        var response = new UserInfoResponse(1L, "u", "n", null, null, "r", null, Set.of());
        assertNull(response.phone());
    }

    @Test
    @DisplayName("email 可为 null")
    void shouldAcceptNullEmail() {
        var response = new UserInfoResponse(1L, "u", "n", null, null, "r", null, Set.of());
        assertNull(response.email());
    }
}
