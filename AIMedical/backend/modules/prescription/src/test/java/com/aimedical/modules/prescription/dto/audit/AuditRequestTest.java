package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditRequestTest {

    @Test
    void shouldSetAndGetFields() {
        AuditRequest req = new AuditRequest();
        req.setPrescriptionId("rx-001");
        req.setPrescriptionItems(List.of(new PrescriptionItem()));
        req.setPatientInfo(new PatientInfo());

        assertEquals("rx-001", req.getPrescriptionId());
        assertEquals(1, req.getPrescriptionItems().size());
        assertNotNull(req.getPatientInfo());
    }
}
