package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrugInteractionTest {

    @Test
    void shouldSetAndGetFields() {
        DrugInteraction interaction = new DrugInteraction();
        interaction.setDrugPair("A-B");
        interaction.setSeverity("HIGH");
        interaction.setDescription("Interaction description");

        assertEquals("A-B", interaction.getDrugPair());
        assertEquals("HIGH", interaction.getSeverity());
        assertEquals("Interaction description", interaction.getDescription());
    }
}
