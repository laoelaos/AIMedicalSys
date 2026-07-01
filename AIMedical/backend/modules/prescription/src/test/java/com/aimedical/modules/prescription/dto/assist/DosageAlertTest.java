package com.aimedical.modules.prescription.dto.assist;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class DosageAlertTest {

    @Test
    void shouldSetAndGetFields() {
        DosageAlert alert = new DosageAlert();
        alert.setAlertLevel(DosageAlertLevel.CRITICAL);
        alert.setWarningType(DoseWarningType.OVER_SINGLE_DOSE);
        alert.setMessage("剂量超限");
        alert.setDrugCode("drug-001");
        alert.setCurrentDose(500.0);
        alert.setSuggestedValue(BigDecimal.valueOf(200));
        alert.setErrorCode("RX_ASSIST_DOSE_STANDARD_NOT_FOUND");

        assertEquals(DosageAlertLevel.CRITICAL, alert.getAlertLevel());
        assertEquals(DoseWarningType.OVER_SINGLE_DOSE, alert.getWarningType());
        assertEquals("剂量超限", alert.getMessage());
        assertEquals("drug-001", alert.getDrugCode());
        assertEquals(500.0, alert.getCurrentDose(), 0.001);
        assertEquals(BigDecimal.valueOf(200), alert.getSuggestedValue());
        assertEquals("RX_ASSIST_DOSE_STANDARD_NOT_FOUND", alert.getErrorCode());
    }
}
