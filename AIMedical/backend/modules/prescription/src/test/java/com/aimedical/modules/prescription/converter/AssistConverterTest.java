package com.aimedical.modules.prescription.converter;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.dto.prescription.DoseWarningItem;
import com.aimedical.modules.prescription.dto.assist.*;
import com.aimedical.modules.prescription.dto.audit.AllergyDetail;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssistConverterTest {

    private final AssistConverter converter = new AssistConverter();

    @Test
    void shouldMapBizRequestToAiRequest() {
        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setPatientId("pat-001");
        patientInfo.setAge(30);
        patientInfo.setGender("M");
        patientInfo.setAllergyHistory("青霉素过敏");
        AllergyDetail detail = new AllergyDetail();
        detail.setAllergen("青霉素");
        patientInfo.setAllergyDetails(List.of(detail));

        PrescriptionAssistRequest biz = new PrescriptionAssistRequest();
        biz.setDiagnosis("感冒");
        biz.setPatientInfo(patientInfo);
        biz.setExistingPrescription("现有处方");
        biz.setPrescriptionId("rx-001");
        biz.setEncounterId("enc-001");

        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest aiReq =
                converter.toAiPrescriptionAssistRequest(biz);

        assertEquals("感冒", aiReq.getDiagnosis());
        assertEquals("现有处方", aiReq.getExistingPrescription());
        assertEquals("rx-001", aiReq.getPrescriptionId());
        assertEquals("enc-001", aiReq.getEncounterId());
        assertNotNull(aiReq.getPatientInfo());
        assertEquals("pat-001", aiReq.getPatientInfo().getPatientId());
    }

    @Test
    void shouldMapAiResultToEmptyResponseWhenNullData() {
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.failure("ERR");

        PrescriptionAssistResponse biz = converter.toPrescriptionAssistResponse(aiResult);

        assertEquals("", biz.getPrescriptionDraft());
        assertTrue(biz.getDoseWarnings().isEmpty());
        assertTrue(biz.getAllergyWarnings().isEmpty());
        assertTrue(biz.isDisclaimerRequired());
    }

    @Test
    void shouldMapAiResultToFullResponse() {
        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse aiResp =
                new com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse();
        aiResp.setPrescriptionDraft("{\"drugs\":[]}");
        aiResp.setDisclaimerRequired(true);
        aiResp.setErrorCode(null);

        DoseWarningItem dwi = new DoseWarningItem();
        dwi.setDrugId("drug-001");
        dwi.setWarningType("OVER_SINGLE_DOSE");
        dwi.setMessage("超量");
        dwi.setSeverity("WARNING");
        aiResp.setDoseWarnings(List.of(dwi));

        com.aimedical.modules.ai.api.dto.prescription.AllergyWarningItem awi =
                new com.aimedical.modules.ai.api.dto.prescription.AllergyWarningItem();
        awi.setDrugId("drug-001");
        awi.setAllergen("青霉素");
        awi.setSeverity("HIGH");
        aiResp.setAllergyWarnings(List.of(awi));

        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.success(aiResp);

        PrescriptionAssistResponse biz = converter.toPrescriptionAssistResponse(aiResult);

        assertEquals("{\"drugs\":[]}", biz.getPrescriptionDraft());
        assertEquals(1, biz.getDoseWarnings().size());
        assertEquals(DoseWarningType.OVER_SINGLE_DOSE, biz.getDoseWarnings().get(0).getWarningType());
        assertEquals(DosageAlertLevel.WARNING, biz.getDoseWarnings().get(0).getSeverity());
        assertEquals(1, biz.getAllergyWarnings().size());
        assertEquals(AllergyWarningSeverity.HIGH, biz.getAllergyWarnings().get(0).getSeverity());
        assertTrue(biz.isDisclaimerRequired());
    }

    @Test
    void shouldMapDoseWarningWithNullWarningTypeFallback() {
        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse aiResp =
                new com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse();
        aiResp.setPrescriptionDraft("{}");
        aiResp.setDisclaimerRequired(false);

        DoseWarningItem dwi = new DoseWarningItem();
        dwi.setDrugId("drug-001");
        dwi.setWarningType(null);
        dwi.setSeverity(null);
        aiResp.setDoseWarnings(List.of(dwi));

        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.success(aiResp);

        PrescriptionAssistResponse biz = converter.toPrescriptionAssistResponse(aiResult);

        assertEquals(DoseWarningType.OVER_SINGLE_DOSE, biz.getDoseWarnings().get(0).getWarningType());
        assertEquals(DosageAlertLevel.INFO, biz.getDoseWarnings().get(0).getSeverity());
    }

    @Test
    void shouldMapAllergyWarningWithInvalidSeverityFallback() {
        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse aiResp =
                new com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse();
        aiResp.setPrescriptionDraft("{}");
        aiResp.setDisclaimerRequired(false);

        com.aimedical.modules.ai.api.dto.prescription.AllergyWarningItem awi =
                new com.aimedical.modules.ai.api.dto.prescription.AllergyWarningItem();
        awi.setDrugId("drug-001");
        awi.setSeverity("UNKNOWN");
        aiResp.setAllergyWarnings(List.of(awi));

        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.success(aiResp);

        PrescriptionAssistResponse biz = converter.toPrescriptionAssistResponse(aiResult);

        assertEquals(AllergyWarningSeverity.INFO, biz.getAllergyWarnings().get(0).getSeverity());
    }
}
