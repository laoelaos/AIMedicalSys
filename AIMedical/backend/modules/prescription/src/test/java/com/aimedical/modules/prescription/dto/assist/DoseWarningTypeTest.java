package com.aimedical.modules.prescription.dto.assist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoseWarningTypeTest {

    @Test
    void shouldHaveAllValues() {
        assertEquals(3, DoseWarningType.values().length);
        assertEquals(DoseWarningType.OVER_SINGLE_DOSE, DoseWarningType.valueOf("OVER_SINGLE_DOSE"));
        assertEquals(DoseWarningType.OVER_DAILY_DOSE, DoseWarningType.valueOf("OVER_DAILY_DOSE"));
        assertEquals(DoseWarningType.OVER_DURATION, DoseWarningType.valueOf("OVER_DURATION"));
    }
}
