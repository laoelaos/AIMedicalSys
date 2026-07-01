package com.aimedical.modules.commonmodule.doctor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DoctorFacadeTest {

    @Test
    void shouldReturnAvailableDoctorsForDepartment() {
        DoctorFacade facade = departmentId -> List.of(
                new AvailableDoctor("D001", "Dr. Smith", departmentId, 5),
                new AvailableDoctor("D002", "Dr. Jones", departmentId, 3)
        );
        List<AvailableDoctor> result = facade.findAvailableDoctorsByDepartment("DEP001");
        assertEquals(2, result.size());
        assertEquals("D001", result.get(0).doctorId());
        assertEquals("D002", result.get(1).doctorId());
    }

    @Test
    void shouldReturnEmptyListWhenNoDoctorsAvailable() {
        DoctorFacade facade = departmentId -> List.of();
        List<AvailableDoctor> result = facade.findAvailableDoctorsByDepartment("DEP999");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
