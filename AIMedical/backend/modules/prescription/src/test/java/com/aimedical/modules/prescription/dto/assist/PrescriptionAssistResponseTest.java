package com.aimedical.modules.prescription.dto.assist;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PrescriptionAssistResponseTest {

    @Test
    void shouldSetAndGetFields() {
        PrescriptionAssistResponse resp = new PrescriptionAssistResponse();
        resp.setPrescriptionDraft("{\"drugs\":[]}");
        resp.setDoseWarnings(List.of(new DoseWarning()));
        resp.setAllergyWarnings(List.of(new AllergyWarningItem()));
        resp.setErrorCode("RX_ASSIST_AI_NO_RECOMMENDATION");
        resp.setDisclaimerRequired(true);
        resp.setPrescriptionId("rx-001");

        assertEquals("{\"drugs\":[]}", resp.getPrescriptionDraft());
        assertEquals(1, resp.getDoseWarnings().size());
        assertEquals(1, resp.getAllergyWarnings().size());
        assertEquals("RX_ASSIST_AI_NO_RECOMMENDATION", resp.getErrorCode());
        assertTrue(resp.isDisclaimerRequired());
        assertEquals("rx-001", resp.getPrescriptionId());
    }
}
