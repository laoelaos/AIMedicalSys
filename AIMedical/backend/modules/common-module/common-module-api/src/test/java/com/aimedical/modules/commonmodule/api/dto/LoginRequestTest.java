package com.aimedical.modules.commonmodule.api.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    @Test
    void shouldSetAndGetPhone() {
        LoginRequest request = new LoginRequest();
        request.setPhone("13800000000");
        assertEquals("13800000000", request.getPhone());
    }

    @Test
    void shouldSetAndGetPassword() {
        LoginRequest request = new LoginRequest();
        request.setPassword("pwd123");
        assertEquals("pwd123", request.getPassword());
    }
}
