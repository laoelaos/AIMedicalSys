package com.aimedical.modules.commonmodule.drug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrugFacadeTest {

    @Test
    void shouldReturnDrugInfoByDrugCode() {
        DrugFacade facade = drugCode -> new DrugInfo(drugCode, "Paracetamol", "500mg", "Tablet", "Bayer", "Box");
        DrugInfo result = facade.findByDrugCode("D001");
        assertNotNull(result);
        assertEquals("D001", result.drugCode());
        assertEquals("Paracetamol", result.drugName());
    }

    @Test
    void shouldReturnNullWhenDrugNotFound() {
        DrugFacade facade = drugCode -> null;
        assertNull(facade.findByDrugCode("UNKNOWN"));
    }
}
