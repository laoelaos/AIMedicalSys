package com.aimedical.modules.prescription.dto.assist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AllergyWarningSeverityTest {

    @Test
    void shouldHaveAllValues() {
        assertEquals(3, AllergyWarningSeverity.values().length);
        assertEquals(AllergyWarningSeverity.INFO, AllergyWarningSeverity.valueOf("INFO"));
        assertEquals(AllergyWarningSeverity.WARNING, AllergyWarningSeverity.valueOf("WARNING"));
        assertEquals(AllergyWarningSeverity.HIGH, AllergyWarningSeverity.valueOf("HIGH"));
    }
}
