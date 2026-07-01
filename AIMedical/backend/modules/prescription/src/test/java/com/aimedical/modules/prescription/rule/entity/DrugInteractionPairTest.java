package com.aimedical.modules.prescription.rule.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrugInteractionPairTest {

    @Test
    void shouldSetAndGetFields() {
        DrugInteractionPair pair = new DrugInteractionPair();
        pair.setId(1L);
        pair.setDrugCodeA("drug-001");
        pair.setDrugCodeB("drug-002");
        pair.setSeverity("HIGH");
        pair.setDescription("Interaction description");

        assertEquals(1L, pair.getId());
        assertEquals("drug-001", pair.getDrugCodeA());
        assertEquals("drug-002", pair.getDrugCodeB());
        assertEquals("HIGH", pair.getSeverity());
        assertEquals("Interaction description", pair.getDescription());
    }
}
