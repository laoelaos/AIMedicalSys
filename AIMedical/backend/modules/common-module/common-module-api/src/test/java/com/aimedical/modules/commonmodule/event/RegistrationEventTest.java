package com.aimedical.modules.commonmodule.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationEventTest {

    @Test
    void shouldConstructViaAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        RegistrationEvent event = new RegistrationEvent(1L, "P001", "S001", "DEP001", "Cardiology", 10L, now);
        assertEquals(1L, event.getRegistrationId());
        assertEquals("P001", event.getPatientId());
        assertEquals("S001", event.getSessionId());
        assertEquals("DEP001", event.getDepartmentId());
        assertEquals("Cardiology", event.getDepartmentName());
        assertEquals(10L, event.getDoctorId());
        assertEquals(now, event.getEventTime());
    }

    @Test
    void shouldConstructViaNoArgsConstructor() {
        RegistrationEvent event = new RegistrationEvent();
        assertNull(event.getRegistrationId());
        assertNull(event.getPatientId());
        assertNull(event.getSessionId());
        assertNull(event.getDepartmentId());
        assertNull(event.getDepartmentName());
        assertNull(event.getDoctorId());
        assertNull(event.getEventTime());
    }

    @Test
    void shouldSupportGettersAndSetters() {
        LocalDateTime now = LocalDateTime.now();
        RegistrationEvent event = new RegistrationEvent();

        event.setRegistrationId(2L);
        assertEquals(2L, event.getRegistrationId());

        event.setPatientId("P002");
        assertEquals("P002", event.getPatientId());

        event.setSessionId("S002");
        assertEquals("S002", event.getSessionId());

        event.setDepartmentId("DEP002");
        assertEquals("DEP002", event.getDepartmentId());

        event.setDepartmentName("Neurology");
        assertEquals("Neurology", event.getDepartmentName());

        event.setDoctorId(20L);
        assertEquals(20L, event.getDoctorId());

        event.setEventTime(now);
        assertEquals(now, event.getEventTime());
    }

    @Test
    void shouldAllowNullSessionId() {
        LocalDateTime now = LocalDateTime.now();
        RegistrationEvent event = new RegistrationEvent(1L, "P001", null, "DEP001", "Cardiology", null, now);
        assertNull(event.getSessionId());
        assertNull(event.getDoctorId());
        assertNotNull(event.getRegistrationId());
        assertNotNull(event.getPatientId());
    }

    @Test
    void shouldMutateFieldsIndependently() {
        RegistrationEvent event = new RegistrationEvent(1L, "P001", "S001", "DEP001", "Cardiology", 10L, LocalDateTime.now());

        event.setSessionId("S002");
        assertEquals("S002", event.getSessionId());
        assertEquals("P001", event.getPatientId());
        assertEquals("DEP001", event.getDepartmentId());
    }
}
