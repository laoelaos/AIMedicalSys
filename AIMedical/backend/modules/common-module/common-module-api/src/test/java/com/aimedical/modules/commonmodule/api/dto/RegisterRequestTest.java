package com.aimedical.modules.commonmodule.api.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    @Test
    void shouldSetAndGetPhone() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800000000");
        assertEquals("13800000000", request.getPhone());
    }

    @Test
    void shouldSetAndGetPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setPassword("Pass123");
        assertEquals("Pass123", request.getPassword());
    }

    @Test
    void shouldSetAndGetName() {
        RegisterRequest request = new RegisterRequest();
        request.setName("张三");
        assertEquals("张三", request.getName());
    }

    @Test
    void shouldSetAndGetGender() {
        RegisterRequest request = new RegisterRequest();
        request.setGender("男");
        assertEquals("男", request.getGender());
    }

    @Test
    void shouldSetAndGetAge() {
        RegisterRequest request = new RegisterRequest();
        request.setAge(25);
        assertEquals(25, request.getAge());
    }
}
