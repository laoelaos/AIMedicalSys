package com.aimedical.modules.commonmodule.doctor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AvailableDoctorTest {

    @Test
    void shouldExposeDoctorId() {
        AvailableDoctor doctor = new AvailableDoctor("D001", "Dr. Smith", "DEP001", 5);
        assertEquals("D001", doctor.doctorId());
    }

    @Test
    void shouldExposeDoctorName() {
        AvailableDoctor doctor = new AvailableDoctor("D001", "Dr. Smith", "DEP001", 5);
        assertEquals("Dr. Smith", doctor.doctorName());
    }

    @Test
    void shouldExposeDepartmentId() {
        AvailableDoctor doctor = new AvailableDoctor("D001", "Dr. Smith", "DEP001", 5);
        assertEquals("DEP001", doctor.departmentId());
    }

    @Test
    void shouldExposeAvailableSlotCount() {
        AvailableDoctor doctor = new AvailableDoctor("D001", "Dr. Smith", "DEP001", 5);
        assertEquals(5, doctor.availableSlotCount());
    }

    @Test
    void shouldSupportZeroSlotCount() {
        AvailableDoctor doctor = new AvailableDoctor("D001", "Dr. Smith", "DEP001", 0);
        assertEquals(0, doctor.availableSlotCount());
    }

    @Test
    void shouldSupportNegativeSlotCount() {
        AvailableDoctor doctor = new AvailableDoctor("D001", "Dr. Smith", "DEP001", -1);
        assertEquals(-1, doctor.availableSlotCount());
    }

    @Test
    void shouldAllowNullFields() {
        AvailableDoctor doctor = new AvailableDoctor(null, null, null, 0);
        assertNull(doctor.doctorId());
        assertNull(doctor.doctorName());
        assertNull(doctor.departmentId());
        assertEquals(0, doctor.availableSlotCount());
    }

    @Test
    void shouldImplementEquality() {
        AvailableDoctor d1 = new AvailableDoctor("D001", "Dr. Smith", "DEP001", 5);
        AvailableDoctor d2 = new AvailableDoctor("D001", "Dr. Smith", "DEP001", 5);
        assertEquals(d1, d2);
    }

    @Test
    void shouldImplementInequality() {
        AvailableDoctor d1 = new AvailableDoctor("D001", "Dr. Smith", "DEP001", 5);
        AvailableDoctor d2 = new AvailableDoctor("D002", "Dr. Jones", "DEP001", 3);
        assertNotEquals(d1, d2);
    }
}
