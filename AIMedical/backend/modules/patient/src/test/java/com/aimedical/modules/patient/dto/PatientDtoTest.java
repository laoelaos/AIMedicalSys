package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientDtoTest {

    @Test
    void shouldSetAndGetId() {
        PatientDto dto = new PatientDto();
        dto.setId(1L);
        assertEquals(1L, dto.getId());
    }

    @Test
    void shouldSetAndGetUserId() {
        PatientDto dto = new PatientDto();
        dto.setUserId(10L);
        assertEquals(10L, dto.getUserId());
    }

    @Test
    void shouldSetAndGetName() {
        PatientDto dto = new PatientDto();
        dto.setName("张三");
        assertEquals("张三", dto.getName());
    }

    @Test
    void shouldSetAndGetPhone() {
        PatientDto dto = new PatientDto();
        dto.setPhone("13800000000");
        assertEquals("13800000000", dto.getPhone());
    }

    @Test
    void shouldSetAndGetGender() {
        PatientDto dto = new PatientDto();
        dto.setGender("男");
        assertEquals("男", dto.getGender());
    }

    @Test
    void shouldSetAndGetAge() {
        PatientDto dto = new PatientDto();
        dto.setAge(30);
        assertEquals(30, dto.getAge());
    }

    @Test
    void shouldSetAndGetEmail() {
        PatientDto dto = new PatientDto();
        dto.setEmail("test@example.com");
        assertEquals("test@example.com", dto.getEmail());
    }

    @Test
    void shouldSetAndGetEmergencyContact() {
        PatientDto dto = new PatientDto();
        dto.setEmergencyContact("李四-13800000001");
        assertEquals("李四-13800000001", dto.getEmergencyContact());
    }

    @Test
    void shouldSetAndGetAvatarUrl() {
        PatientDto dto = new PatientDto();
        dto.setAvatarUrl("http://example.com/avatar.png");
        assertEquals("http://example.com/avatar.png", dto.getAvatarUrl());
    }
}
