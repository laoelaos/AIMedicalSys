package com.aimedical.modules.prescription.dto.assist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoseWarningTest {

    @Test
    void shouldSetAndGetFields() {
        DoseWarning dw = new DoseWarning();
        dw.setDrugId("drug-001");
        dw.setWarningType(DoseWarningType.OVER_SINGLE_DOSE);
        dw.setMessage("单次剂量超限");
        dw.setSeverity(DosageAlertLevel.WARNING);

        assertEquals("drug-001", dw.getDrugId());
        assertEquals(DoseWarningType.OVER_SINGLE_DOSE, dw.getWarningType());
        assertEquals("单次剂量超限", dw.getMessage());
        assertEquals(DosageAlertLevel.WARNING, dw.getSeverity());
    }
}
