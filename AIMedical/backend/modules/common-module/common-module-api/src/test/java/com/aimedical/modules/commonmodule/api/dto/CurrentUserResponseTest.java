package com.aimedical.modules.commonmodule.api.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserResponseTest {

    @Test
    void shouldSetAndGetUserId() {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setUserId(1L);
        assertEquals(1L, response.getUserId());
    }

    @Test
    void shouldSetAndGetUsername() {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setUsername("alice");
        assertEquals("alice", response.getUsername());
    }

    @Test
    void shouldSetAndGetNickname() {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setNickname("Alice");
        assertEquals("Alice", response.getNickname());
    }

    @Test
    void shouldSetAndGetPhone() {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setPhone("13800000000");
        assertEquals("13800000000", response.getPhone());
    }

    @Test
    void shouldSetAndGetGender() {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setGender("女");
        assertEquals("女", response.getGender());
    }

    @Test
    void shouldSetAndGetAge() {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setAge(30);
        assertEquals(30, response.getAge());
    }

    @Test
    void shouldSetAndGetUserType() {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setUserType("DOCTOR");
        assertEquals("DOCTOR", response.getUserType());
    }

    @Test
    void shouldSetAndGetRoles() {
        CurrentUserResponse response = new CurrentUserResponse();
        List<String> roles = List.of("admin", "doctor");
        response.setRoles(roles);
        assertEquals(roles, response.getRoles());
    }
}
