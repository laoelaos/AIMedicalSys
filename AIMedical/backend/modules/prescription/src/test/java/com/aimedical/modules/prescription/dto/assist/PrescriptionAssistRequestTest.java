package com.aimedical.modules.prescription.dto.assist;

import com.aimedical.modules.ai.api.dto.prescription.ExamResultItem;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PrescriptionAssistRequestTest {

    @Test
    void shouldSetAndGetFields() {
        PrescriptionAssistRequest req = new PrescriptionAssistRequest();
        req.setDiagnosis("感冒");
        req.setExamResults(List.of(new ExamResultItem()));
        req.setPatientInfo(new PatientInfo());
        req.setExistingPrescription("现有处方");
        req.setPrescriptionId("rx-001");
        req.setEncounterId("enc-001");

        assertEquals("感冒", req.getDiagnosis());
        assertEquals(1, req.getExamResults().size());
        assertNotNull(req.getPatientInfo());
        assertEquals("现有处方", req.getExistingPrescription());
        assertEquals("rx-001", req.getPrescriptionId());
        assertEquals("enc-001", req.getEncounterId());
    }
}
