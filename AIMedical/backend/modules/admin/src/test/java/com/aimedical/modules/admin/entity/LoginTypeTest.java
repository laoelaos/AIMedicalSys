package com.aimedical.modules.admin.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginTypeTest {

    @Test
    void shouldDefineThreeLoginTypes() {
        assertEquals(3, LoginType.values().length);
        assertNotNull(LoginType.valueOf("LOGIN"));
        assertNotNull(LoginType.valueOf("LOGOUT"));
        assertNotNull(LoginType.valueOf("REFRESH"));
    }

    @Test
    void shouldExposeCodeAndDescForLogin() {
        assertEquals("LOGIN", LoginType.LOGIN.getCode());
        assertEquals("登录", LoginType.LOGIN.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForLogout() {
        assertEquals("LOGOUT", LoginType.LOGOUT.getCode());
        assertEquals("登出", LoginType.LOGOUT.getDesc());
    }

    @Test
    void shouldExposeCodeAndDescForRefresh() {
        assertEquals("REFRESH", LoginType.REFRESH.getCode());
        assertEquals("刷新令牌", LoginType.REFRESH.getDesc());
    }
}