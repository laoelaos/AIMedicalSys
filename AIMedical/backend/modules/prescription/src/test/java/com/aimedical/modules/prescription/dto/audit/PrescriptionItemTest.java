package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionItemTest {

    @Test
    void shouldSetAndGetFields() {
        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDrugName("Aspirin");
        item.setDose(BigDecimal.valueOf(100.0));
        item.setFrequency("tid");
        item.setDuration("7d");
        item.setRoute("oral");

        assertEquals("drug-001", item.getDrugId());
        assertEquals("Aspirin", item.getDrugName());
        assertEquals(BigDecimal.valueOf(100.0), item.getDose());
        assertEquals("tid", item.getFrequency());
        assertEquals("7d", item.getDuration());
        assertEquals("oral", item.getRoute());
    }
}
