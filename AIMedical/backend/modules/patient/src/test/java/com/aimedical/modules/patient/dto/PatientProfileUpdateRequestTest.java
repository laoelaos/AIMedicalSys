package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientProfileUpdateRequestTest {

    @Test
    void shouldSetAndGetName() {
        PatientProfileUpdateRequest r = new PatientProfileUpdateRequest();
        r.setName("张三");
        assertEquals("张三", r.getName());
    }

    @Test
    void shouldSetAndGetPhone() {
        PatientProfileUpdateRequest r = new PatientProfileUpdateRequest();
        r.setPhone("13800000000");
        assertEquals("13800000000", r.getPhone());
    }

    @Test
    void shouldSetAndGetGender() {
        PatientProfileUpdateRequest r = new PatientProfileUpdateRequest();
        r.setGender("男");
        assertEquals("男", r.getGender());
    }

    @Test
    void shouldSetAndGetAge() {
        PatientProfileUpdateRequest r = new PatientProfileUpdateRequest();
        r.setAge(30);
        assertEquals(30, r.getAge());
    }

    @Test
    void shouldSetAndGetEmail() {
        PatientProfileUpdateRequest r = new PatientProfileUpdateRequest();
        r.setEmail("test@example.com");
        assertEquals("test@example.com", r.getEmail());
    }

    @Test
    void shouldSetAndGetEmergencyContact() {
        PatientProfileUpdateRequest r = new PatientProfileUpdateRequest();
        r.setEmergencyContact("李四-13800000001");
        assertEquals("李四-13800000001", r.getEmergencyContact());
    }
}
