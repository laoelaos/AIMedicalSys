package com.aimedical.modules.prescription.dto.assist;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class DosageCheckRequestTest {

    @Test
    void shouldSetAndGetFields() {
        DosageCheckRequest req = new DosageCheckRequest();
        req.setPrescriptionId("rx-001");
        req.setDrugCode("drug-001");
        req.setDosage(100.0);
        req.setUnit("mg");
        req.setRouteOfAdministration("oral");
        req.setPatientAge(30);
        req.setPatientWeight(BigDecimal.valueOf(70.0));
        req.setFrequency("tid");

        assertEquals("rx-001", req.getPrescriptionId());
        assertEquals("drug-001", req.getDrugCode());
        assertEquals(100.0, req.getDosage(), 0.001);
        assertEquals("mg", req.getUnit());
        assertEquals("oral", req.getRouteOfAdministration());
        assertEquals(30, req.getPatientAge());
        assertEquals(BigDecimal.valueOf(70.0), req.getPatientWeight());
        assertEquals("tid", req.getFrequency());
    }
}
