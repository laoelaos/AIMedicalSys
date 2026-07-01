package com.aimedical.modules.prescription.dto.assist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AllergyWarningItemTest {

    @Test
    void shouldSetAndGetFields() {
        AllergyWarningItem item = new AllergyWarningItem();
        item.setDrugId("drug-001");
        item.setAllergen("青霉素");
        item.setSeverity(AllergyWarningSeverity.HIGH);

        assertEquals("drug-001", item.getDrugId());
        assertEquals("青霉素", item.getAllergen());
        assertEquals(AllergyWarningSeverity.HIGH, item.getSeverity());
    }
}
