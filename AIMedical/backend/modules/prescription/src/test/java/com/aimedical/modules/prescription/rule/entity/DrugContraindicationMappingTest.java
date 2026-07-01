package com.aimedical.modules.prescription.rule.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrugContraindicationMappingTest {

    @Test
    void shouldSetAndGetFields() {
        DrugContraindicationMapping mapping = new DrugContraindicationMapping();
        mapping.setDrugCode("drug-001");
        mapping.setContraindications("[{\"diseaseName\":\"Diabetes\",\"level\":\"ABSOLUTE_CONTRAINDICATION\"}]");

        assertEquals("drug-001", mapping.getDrugCode());
        assertEquals("[{\"diseaseName\":\"Diabetes\",\"level\":\"ABSOLUTE_CONTRAINDICATION\"}]", mapping.getContraindications());
    }

    @Test
    void shouldInheritBaseEntityFields() {
        DrugContraindicationMapping mapping = new DrugContraindicationMapping();
        assertNull(mapping.getId());
    }
}
