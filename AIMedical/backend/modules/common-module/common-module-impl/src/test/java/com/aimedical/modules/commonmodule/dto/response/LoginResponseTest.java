package com.aimedical.modules.commonmodule.dto.response;

import com.aimedical.modules.commonmodule.auth.UserInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginResponse")
class LoginResponseTest {

    @Test
    @DisplayName("构造并访问所有字段")
    void shouldConstructAndAccessFields() {
        var userInfo = new UserInfoResponse(1L, "testuser", "测试", "13800138000",
                "test@test.com", "DOCTOR", "DOC_GENERAL", Set.of("perm1"));
        var response = new LoginResponse(1L, "testuser", "access-token", "refresh-token",
                "Bearer", 86400L, false, userInfo);

        assertEquals(1L, response.userId());
        assertEquals("testuser", response.username());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(86400L, response.expiresIn());
        assertFalse(response.passwordChangeRequired());
        assertSame(userInfo, response.user());
    }

    @Test
    @DisplayName("expiresIn 为 long 类型（非装箱）")
    void shouldHavePrimitiveLongExpiresIn() {
        var userInfo = new UserInfoResponse(1L, "u", "n", null, null, "r", null, Set.of());
        var response = new LoginResponse(1L, "u", "t", null, "Bearer", 900L, false, userInfo);
        assertEquals(900L, response.expiresIn());
    }

    @Test
    @DisplayName("passwordChangeRequired 为 boolean 类型（非装箱）")
    void shouldHavePrimitiveBooleanPasswordChangeRequired() {
        var userInfo = new UserInfoResponse(1L, "u", "n", null, null, "r", null, Set.of());
        var response = new LoginResponse(1L, "u", "t", null, "Bearer", 900L, true, userInfo);
        assertTrue(response.passwordChangeRequired());
    }

    @Test
    @DisplayName("user 不为 null 时正常返回")
    void shouldReturnNonNullUser() {
        var userInfo = new UserInfoResponse(1L, "u", "n", null, null, "r", null, Set.of());
        var response = new LoginResponse(1L, "u", "t", null, "Bearer", 900L, false, userInfo);
        assertNotNull(response.user());
    }
}
