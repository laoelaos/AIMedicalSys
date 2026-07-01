package com.aimedical.modules.prescription.converter;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.dto.prescription.AlertItem;
import com.aimedical.modules.ai.api.dto.prescription.DrugInteractionItem;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckItem;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckRequest;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckResponse;
import com.aimedical.modules.ai.api.dto.prescription.SuggestionItem;
import com.aimedical.modules.prescription.dto.audit.AllergyDetail;
import com.aimedical.modules.prescription.dto.audit.AuditAlert;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.AuditResponse;
import com.aimedical.modules.prescription.dto.audit.DrugInteraction;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.dto.audit.Suggestion;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditConverterTest {

    private final AuditConverter converter = new AuditConverter();

    @Test
    void shouldMapAuditRequestToPrescriptionCheckRequest() {
        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDrugName("Aspirin");
        item.setDose(BigDecimal.valueOf(100));
        item.setFrequency("tid");
        item.setDuration("7d");
        item.setRoute("oral");

        AllergyDetail allergy = new AllergyDetail();
        allergy.setAllergen("Penicillin");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setPatientId("pat-001");
        patientInfo.setAge(30);
        patientInfo.setGender("M");
        patientInfo.setAllergyHistory("None");
        patientInfo.setAllergyDetails(List.of(allergy));
        patientInfo.setComorbidities(List.of("Diabetes"));

        AuditRequest request = new AuditRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        PrescriptionCheckRequest checkRequest = converter.toAiPrescriptionCheckRequest(request);

        assertEquals("rx-001", checkRequest.getPrescriptionId());
        assertEquals(1, checkRequest.getPrescriptionItems().size());
        PrescriptionCheckItem checkItem = checkRequest.getPrescriptionItems().get(0);
        assertEquals("drug-001", checkItem.getDrugId());
        assertEquals("Aspirin", checkItem.getDrugName());
        assertEquals(100.0, checkItem.getDose(), 0.001);
        assertEquals("tid", checkItem.getFrequency());
        assertEquals("7d", checkItem.getDuration());
        assertEquals("oral", checkItem.getRoute());
        assertNotNull(checkRequest.getPatientInfo());
    }

    @Test
    void shouldMapUnitFieldToAiCheckItem() {
        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");
        item.setUnit("mg");

        PatientInfo patientInfo = new PatientInfo();
        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        PrescriptionCheckRequest checkRequest = converter.toAiPrescriptionCheckRequest(request);

        assertEquals("mg", checkRequest.getPrescriptionItems().get(0).getUnit());
    }

    @Test
    void shouldMapWeightFieldToAiPatientInfo() {
        com.aimedical.modules.prescription.dto.audit.PatientInfo bizPatient =
                new com.aimedical.modules.prescription.dto.audit.PatientInfo();
        bizPatient.setPatientId("pat-001");
        bizPatient.setWeight(75.5);

        AuditRequest request = new AuditRequest();
        request.setPatientInfo(bizPatient);
        PrescriptionItem item = new PrescriptionItem();
        item.setDose(BigDecimal.valueOf(100));
        request.setPrescriptionItems(List.of(item));

        PrescriptionCheckRequest checkRequest = converter.toAiPrescriptionCheckRequest(request);

        assertNotNull(checkRequest.getPatientInfo());
        assertEquals(75.5, checkRequest.getPatientInfo().getWeight(), 0.001);
    }

    @Test
    void shouldMapUnitAsNullWhenNotSet() {
        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        PrescriptionCheckRequest checkRequest = converter.toAiPrescriptionCheckRequest(request);

        assertNull(checkRequest.getPrescriptionItems().get(0).getUnit());
    }

    @Test
    void shouldMapWeightAsNullWhenNotSet() {
        com.aimedical.modules.prescription.dto.audit.PatientInfo bizPatient =
                new com.aimedical.modules.prescription.dto.audit.PatientInfo();
        bizPatient.setPatientId("pat-001");

        AuditRequest request = new AuditRequest();
        request.setPatientInfo(bizPatient);
        PrescriptionItem item = new PrescriptionItem();
        item.setDose(BigDecimal.valueOf(100));
        request.setPrescriptionItems(List.of(item));

        PrescriptionCheckRequest checkRequest = converter.toAiPrescriptionCheckRequest(request);

        assertNull(checkRequest.getPatientInfo().getWeight());
    }

    @Test
    void shouldMapAiResponseToAuditResponseWithLowRisk() {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("LOW");
        checkResponse.setAlerts(List.of());
        checkResponse.setInteractions(List.of());
        checkResponse.setSuggestions(List.of());
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);

        AuditResponse response = converter.toAuditResponse(aiResult);

        assertEquals(AuditRiskLevel.PASS, response.getRiskLevel());
        assertFalse(response.isFromFallback());
    }

    @Test
    void shouldMapMediumRiskToWarn() {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("MEDIUM");
        checkResponse.setAlerts(List.of());
        checkResponse.setInteractions(List.of());
        checkResponse.setSuggestions(List.of());
        checkResponse.setFromFallback(true);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);

        AuditResponse response = converter.toAuditResponse(aiResult);

        assertEquals(AuditRiskLevel.WARN, response.getRiskLevel());
        assertTrue(response.isFromFallback());
    }

    @Test
    void shouldMapHighRiskToBlock() {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("HIGH");
        checkResponse.setAlerts(List.of());
        checkResponse.setInteractions(List.of());
        checkResponse.setSuggestions(List.of());
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);

        AuditResponse response = converter.toAuditResponse(aiResult);

        assertEquals(AuditRiskLevel.BLOCK, response.getRiskLevel());
    }

    @Test
    void shouldMapNullRiskToPass() {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel(null);
        checkResponse.setAlerts(List.of());
        checkResponse.setInteractions(List.of());
        checkResponse.setSuggestions(List.of());
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);

        AuditResponse response = converter.toAuditResponse(aiResult);

        assertEquals(AuditRiskLevel.PASS, response.getRiskLevel());
    }

    @Test
    void shouldMapAlertsWithSeverity() {
        AlertItem critical = new AlertItem();
        critical.setAlertCode("C001");
        critical.setAlertMessage("Critical alert");
        critical.setSeverity("CRITICAL");

        AlertItem warning = new AlertItem();
        warning.setAlertCode("W001");
        warning.setAlertMessage("Warning alert");
        warning.setSeverity("WARNING");

        AlertItem info = new AlertItem();
        info.setAlertCode("I001");
        info.setAlertMessage("Info alert");
        info.setSeverity("INFO");

        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("LOW");
        checkResponse.setAlerts(List.of(critical, warning, info));
        checkResponse.setInteractions(List.of());
        checkResponse.setSuggestions(List.of());
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);

        AuditResponse response = converter.toAuditResponse(aiResult);

        assertEquals(3, response.getAlerts().size());
        assertEquals(com.aimedical.modules.prescription.dto.audit.AlertSeverity.CRITICAL, response.getAlerts().get(0).getSeverity());
        assertEquals(com.aimedical.modules.prescription.dto.audit.AlertSeverity.WARNING, response.getAlerts().get(1).getSeverity());
        assertEquals(com.aimedical.modules.prescription.dto.audit.AlertSeverity.INFO, response.getAlerts().get(2).getSeverity());
    }

    @Test
    void shouldMapInteractionsAndSuggestions() {
        DrugInteractionItem interactionItem = new DrugInteractionItem();
        interactionItem.setDrugPair("A-B");
        interactionItem.setSeverity("HIGH");
        interactionItem.setDescription("Interaction desc");

        SuggestionItem suggestionItem = new SuggestionItem();
        suggestionItem.setSuggestionCode("S001");
        suggestionItem.setSuggestionText("Monitor");

        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("LOW");
        checkResponse.setAlerts(List.of());
        checkResponse.setInteractions(List.of(interactionItem));
        checkResponse.setSuggestions(List.of(suggestionItem));
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);

        AuditResponse response = converter.toAuditResponse(aiResult);

        assertEquals(1, response.getInteractions().size());
        assertEquals("A-B", response.getInteractions().get(0).getDrugPair());
        assertEquals(1, response.getSuggestions().size());
        assertEquals("S001", response.getSuggestions().get(0).getSuggestionCode());
    }
}
