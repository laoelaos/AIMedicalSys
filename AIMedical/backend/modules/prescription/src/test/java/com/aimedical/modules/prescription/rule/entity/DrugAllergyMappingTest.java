package com.aimedical.modules.prescription.rule.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrugAllergyMappingTest {

    @Test
    void shouldSetAndGetFields() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        mapping.setDrugCode("drug-001");
        mapping.setAllergens("[\"Penicillin\"]");

        assertEquals("drug-001", mapping.getDrugCode());
        assertEquals("[\"Penicillin\"]", mapping.getAllergens());
    }

    @Test
    void shouldInheritBaseEntityFields() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        assertNull(mapping.getId());
    }
}
