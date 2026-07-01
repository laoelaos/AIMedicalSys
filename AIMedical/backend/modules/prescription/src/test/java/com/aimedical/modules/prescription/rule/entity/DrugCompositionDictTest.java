package com.aimedical.modules.prescription.rule.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrugCompositionDictTest {

    @Test
    void shouldSetAndGetFields() {
        DrugCompositionDict dict = new DrugCompositionDict();
        dict.setDrugCode("drug-001");
        dict.setIngredients("[{\"ingredientCode\":\"ING-001\",\"ingredientName\":\"Aspirin\"}]");

        assertEquals("drug-001", dict.getDrugCode());
        assertEquals("[{\"ingredientCode\":\"ING-001\",\"ingredientName\":\"Aspirin\"}]", dict.getIngredients());
    }

    @Test
    void shouldInheritBaseEntityFields() {
        DrugCompositionDict dict = new DrugCompositionDict();
        assertNull(dict.getId());
    }
}
