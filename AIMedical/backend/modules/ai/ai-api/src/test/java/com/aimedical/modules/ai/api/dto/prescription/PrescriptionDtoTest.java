package com.aimedical.modules.ai.api.dto.prescription;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionDtoTest {

    // --- PrescriptionCheckItem ---

    @Test
    void shouldCreatePrescriptionCheckItemWithDefaultConstructor() {
        PrescriptionCheckItem item = new PrescriptionCheckItem();
        assertNull(item.getDrugId());
        assertNull(item.getDrugName());
        assertEquals(0.0, item.getDose(), 0.001);
        assertNull(item.getFrequency());
        assertNull(item.getDuration());
        assertNull(item.getRoute());
    }

    @Test
    void shouldSetAndGetPrescriptionCheckItemFields() {
        PrescriptionCheckItem item = new PrescriptionCheckItem();
        item.setDrugId("DRUG001");
        item.setDrugName("阿莫西林");
        item.setDose(0.5);
        item.setFrequency("tid");
        item.setDuration("7d");
        item.setRoute("口服");
        assertEquals("DRUG001", item.getDrugId());
        assertEquals("阿莫西林", item.getDrugName());
        assertEquals(0.5, item.getDose(), 0.001);
        assertEquals("tid", item.getFrequency());
        assertEquals("7d", item.getDuration());
        assertEquals("口服", item.getRoute());
    }

    // --- AllergyDetailItem ---

    @Test
    void shouldCreateAllergyDetailItemWithDefaultConstructor() {
        AllergyDetailItem item = new AllergyDetailItem();
        assertNull(item.getAllergen());
        assertNull(item.getReactionType());
        assertNull(item.getSeverity());
        assertNull(item.getOccurredAt());
    }

    @Test
    void shouldSetAndGetAllergyDetailItemFields() {
        AllergyDetailItem item = new AllergyDetailItem();
        item.setAllergen("青霉素");
        item.setReactionType("皮疹");
        item.setSeverity("MODERATE");
        item.setOccurredAt("2025-01-15");
        assertEquals("青霉素", item.getAllergen());
        assertEquals("皮疹", item.getReactionType());
        assertEquals("MODERATE", item.getSeverity());
        assertEquals("2025-01-15", item.getOccurredAt());
    }

    // --- DrugInteractionItem ---

    @Test
    void shouldCreateDrugInteractionItemWithDefaultConstructor() {
        DrugInteractionItem item = new DrugInteractionItem();
        assertNull(item.getDrugPair());
        assertNull(item.getSeverity());
        assertNull(item.getDescription());
    }

    @Test
    void shouldSetAndGetDrugInteractionItemFields() {
        DrugInteractionItem item = new DrugInteractionItem();
        item.setDrugPair("阿莫西林-甲氨蝶呤");
        item.setSeverity("WARNING");
        item.setDescription("可能增加甲氨蝶呤毒性");
        assertEquals("阿莫西林-甲氨蝶呤", item.getDrugPair());
        assertEquals("WARNING", item.getSeverity());
        assertEquals("可能增加甲氨蝶呤毒性", item.getDescription());
    }

    // --- AlertItem ---

    @Test
    void shouldCreateAlertItemWithDefaultConstructor() {
        AlertItem item = new AlertItem();
        assertNull(item.getAlertCode());
        assertNull(item.getAlertMessage());
        assertNull(item.getSeverity());
    }

    @Test
    void shouldSetAndGetAlertItemFields() {
        AlertItem item = new AlertItem();
        item.setAlertCode("ALERT-001");
        item.setAlertMessage("患者年龄超过65岁，注意肾功能");
        item.setSeverity("WARNING");
        assertEquals("ALERT-001", item.getAlertCode());
        assertEquals("患者年龄超过65岁，注意肾功能", item.getAlertMessage());
        assertEquals("WARNING", item.getSeverity());
    }

    // --- SuggestionItem ---

    @Test
    void shouldCreateSuggestionItemWithDefaultConstructor() {
        SuggestionItem item = new SuggestionItem();
        assertNull(item.getSuggestionCode());
        assertNull(item.getSuggestionText());
    }

    @Test
    void shouldSetAndGetSuggestionItemFields() {
        SuggestionItem item = new SuggestionItem();
        item.setSuggestionCode("SUG-001");
        item.setSuggestionText("建议餐后服用");
        assertEquals("SUG-001", item.getSuggestionCode());
        assertEquals("建议餐后服用", item.getSuggestionText());
    }

    // --- ExamResultItem ---

    @Test
    void shouldCreateExamResultItemWithDefaultConstructor() {
        ExamResultItem item = new ExamResultItem();
        assertNull(item.getItemName());
        assertNull(item.getItemValue());
        assertNull(item.getReferenceRange());
    }

    @Test
    void shouldSetAndGetExamResultItemFields() {
        ExamResultItem item = new ExamResultItem();
        item.setItemName("白细胞计数");
        item.setItemValue("12.5");
        item.setReferenceRange("4.0-10.0");
        assertEquals("白细胞计数", item.getItemName());
        assertEquals("12.5", item.getItemValue());
        assertEquals("4.0-10.0", item.getReferenceRange());
    }

    // --- DoseWarningItem ---

    @Test
    void shouldCreateDoseWarningItemWithDefaultConstructor() {
        DoseWarningItem item = new DoseWarningItem();
        assertNull(item.getDrugId());
        assertNull(item.getWarningType());
        assertNull(item.getMessage());
        assertNull(item.getSeverity());
    }

    @Test
    void shouldSetAndGetDoseWarningItemFields() {
        DoseWarningItem item = new DoseWarningItem();
        item.setDrugId("DRUG001");
        item.setWarningType("OVER_SINGLE_DOSE");
        item.setMessage("单次剂量超过推荐上限");
        item.setSeverity("WARNING");
        assertEquals("DRUG001", item.getDrugId());
        assertEquals("OVER_SINGLE_DOSE", item.getWarningType());
        assertEquals("单次剂量超过推荐上限", item.getMessage());
        assertEquals("WARNING", item.getSeverity());
    }

    // --- AllergyWarningItem ---

    @Test
    void shouldCreateAllergyWarningItemWithDefaultConstructor() {
        AllergyWarningItem item = new AllergyWarningItem();
        assertNull(item.getDrugId());
        assertNull(item.getAllergen());
        assertNull(item.getSeverity());
    }

    @Test
    void shouldSetAndGetAllergyWarningItemFields() {
        AllergyWarningItem item = new AllergyWarningItem();
        item.setDrugId("DRUG003");
        item.setAllergen("磺胺类");
        item.setSeverity("HIGH");
        assertEquals("DRUG003", item.getDrugId());
        assertEquals("磺胺类", item.getAllergen());
        assertEquals("HIGH", item.getSeverity());
    }

    // --- PatientInfo ---

    @Test
    void shouldCreatePatientInfoWithDefaultConstructor() {
        PatientInfo info = new PatientInfo();
        assertNull(info.getPatientId());
        assertNull(info.getAge());
        assertNull(info.getGender());
        assertNull(info.getAllergyHistory());
        assertNull(info.getAllergyDetails());
        assertNull(info.getComorbidities());
    }

    @Test
    void shouldSetAndGetPatientInfoFields() {
        PatientInfo info = new PatientInfo();
        info.setPatientId("P001");
        info.setAge(35);
        info.setGender("男");
        info.setAllergyHistory("青霉素,磺胺");

        AllergyDetailItem allergy = new AllergyDetailItem();
        allergy.setAllergen("青霉素");
        allergy.setSeverity("SEVERE");
        List<AllergyDetailItem> allergies = new ArrayList<>();
        allergies.add(allergy);
        info.setAllergyDetails(allergies);

        List<String> comorbidities = new ArrayList<>();
        comorbidities.add("高血压");
        info.setComorbidities(comorbidities);

        assertEquals("P001", info.getPatientId());
        assertEquals(35, info.getAge());
        assertEquals("男", info.getGender());
        assertEquals("青霉素,磺胺", info.getAllergyHistory());
        assertEquals(1, info.getAllergyDetails().size());
        assertEquals("青霉素", info.getAllergyDetails().get(0).getAllergen());
        assertEquals(1, info.getComorbidities().size());
        assertEquals("高血压", info.getComorbidities().get(0));
    }

    @Test
    void shouldDefaultPatientInfoAgeToNull() {
        PatientInfo info = new PatientInfo();
        assertNull(info.getAge());
    }

    // --- PrescriptionCheckRequest ---

    @Test
    void shouldCreatePrescriptionCheckRequestWithDefaultConstructor() {
        PrescriptionCheckRequest request = new PrescriptionCheckRequest();
        assertNull(request.getPrescriptionItems());
        assertNull(request.getPatientInfo());
        assertNull(request.getPrescriptionId());
    }

    @Test
    void shouldSetAndGetPrescriptionCheckRequestFields() {
        PrescriptionCheckRequest request = new PrescriptionCheckRequest();

        PrescriptionCheckItem item = new PrescriptionCheckItem();
        item.setDrugId("DRUG001");
        item.setDrugName("阿莫西林");
        item.setDose(0.5);
        List<PrescriptionCheckItem> items = new ArrayList<>();
        items.add(item);
        request.setPrescriptionItems(items);

        PatientInfo patient = new PatientInfo();
        patient.setPatientId("P001");
        request.setPatientInfo(patient);

        request.setPrescriptionId("RX-001");

        assertEquals(1, request.getPrescriptionItems().size());
        assertEquals("DRUG001", request.getPrescriptionItems().get(0).getDrugId());
        assertEquals("P001", request.getPatientInfo().getPatientId());
        assertEquals("RX-001", request.getPrescriptionId());
    }

    // --- PrescriptionCheckResponse ---

    @Test
    void shouldCreatePrescriptionCheckResponseWithDefaultConstructor() {
        PrescriptionCheckResponse response = new PrescriptionCheckResponse();
        assertNull(response.getRiskLevel());
        assertNull(response.getAlerts());
        assertNull(response.getInteractions());
        assertNull(response.getSuggestions());
        assertFalse(response.isFromFallback());
    }

    @Test
    void shouldSetAndGetPrescriptionCheckResponseFields() {
        PrescriptionCheckResponse response = new PrescriptionCheckResponse();
        response.setRiskLevel("HIGH");

        AlertItem alert = new AlertItem();
        alert.setAlertCode("A-001");
        alert.setAlertMessage("肾功能不全");
        List<AlertItem> alerts = List.of(alert);
        response.setAlerts(alerts);

        DrugInteractionItem interaction = new DrugInteractionItem();
        interaction.setDrugPair("阿莫西林-甲氨蝶呤");
        List<DrugInteractionItem> interactions = List.of(interaction);
        response.setInteractions(interactions);

        SuggestionItem suggestion = new SuggestionItem();
        suggestion.setSuggestionText("调整剂量");
        List<SuggestionItem> suggestions = List.of(suggestion);
        response.setSuggestions(suggestions);

        response.setFromFallback(true);

        assertEquals("HIGH", response.getRiskLevel());
        assertEquals(1, response.getAlerts().size());
        assertEquals("A-001", response.getAlerts().get(0).getAlertCode());
        assertEquals(1, response.getInteractions().size());
        assertEquals("阿莫西林-甲氨蝶呤", response.getInteractions().get(0).getDrugPair());
        assertEquals(1, response.getSuggestions().size());
        assertEquals("调整剂量", response.getSuggestions().get(0).getSuggestionText());
        assertTrue(response.isFromFallback());
    }

    @Test
    void shouldDefaultPrescriptionCheckResponseFromFallbackToFalse() {
        PrescriptionCheckResponse response = new PrescriptionCheckResponse();
        assertFalse(response.isFromFallback());
    }

    // --- PrescriptionAssistRequest ---

    @Test
    void shouldCreatePrescriptionAssistRequestWithDefaultConstructor() {
        PrescriptionAssistRequest request = new PrescriptionAssistRequest();
        assertNull(request.getDiagnosis());
        assertNull(request.getExamResults());
        assertNull(request.getPatientInfo());
        assertNull(request.getExistingPrescription());
        assertNull(request.getPrescriptionId());
        assertNull(request.getEncounterId());
    }

    @Test
    void shouldSetAndGetPrescriptionAssistRequestFields() {
        PrescriptionAssistRequest request = new PrescriptionAssistRequest();
        request.setDiagnosis("社区获得性肺炎");

        ExamResultItem exam = new ExamResultItem();
        exam.setItemName("CRP");
        exam.setItemValue("45");
        List<ExamResultItem> exams = List.of(exam);
        request.setExamResults(exams);

        PatientInfo patient = new PatientInfo();
        patient.setPatientId("P001");
        request.setPatientInfo(patient);

        request.setExistingPrescription("{\"drugs\":[]}");
        request.setPrescriptionId("RX-001");
        request.setEncounterId("ENC-001");

        assertEquals("社区获得性肺炎", request.getDiagnosis());
        assertEquals(1, request.getExamResults().size());
        assertEquals("CRP", request.getExamResults().get(0).getItemName());
        assertEquals("P001", request.getPatientInfo().getPatientId());
        assertEquals("{\"drugs\":[]}", request.getExistingPrescription());
        assertEquals("RX-001", request.getPrescriptionId());
        assertEquals("ENC-001", request.getEncounterId());
    }

    // --- PrescriptionAssistResponse ---

    @Test
    void shouldCreatePrescriptionAssistResponseWithDefaultConstructor() {
        PrescriptionAssistResponse response = new PrescriptionAssistResponse();
        assertNull(response.getPrescriptionDraft());
        assertNull(response.getDoseWarnings());
        assertNull(response.getAllergyWarnings());
        assertNull(response.getErrorCode());
        assertFalse(response.isDisclaimerRequired());
    }

    @Test
    void shouldSetAndGetPrescriptionAssistResponseFields() {
        PrescriptionAssistResponse response = new PrescriptionAssistResponse();
        response.setPrescriptionDraft("{\"items\":[]}");

        DoseWarningItem doseWarning = new DoseWarningItem();
        doseWarning.setWarningType("OVER_SINGLE_DOSE");
        List<DoseWarningItem> doseWarnings = List.of(doseWarning);
        response.setDoseWarnings(doseWarnings);

        AllergyWarningItem allergyWarning = new AllergyWarningItem();
        allergyWarning.setAllergen("青霉素");
        List<AllergyWarningItem> allergyWarnings = List.of(allergyWarning);
        response.setAllergyWarnings(allergyWarnings);

        response.setErrorCode("ERR-001");
        response.setDisclaimerRequired(true);

        assertEquals("{\"items\":[]}", response.getPrescriptionDraft());
        assertEquals(1, response.getDoseWarnings().size());
        assertEquals("OVER_SINGLE_DOSE", response.getDoseWarnings().get(0).getWarningType());
        assertEquals(1, response.getAllergyWarnings().size());
        assertEquals("青霉素", response.getAllergyWarnings().get(0).getAllergen());
        assertEquals("ERR-001", response.getErrorCode());
        assertTrue(response.isDisclaimerRequired());
    }

    @Test
    void shouldDefaultPrescriptionAssistResponseDisclaimerRequiredToFalse() {
        PrescriptionAssistResponse response = new PrescriptionAssistResponse();
        assertFalse(response.isDisclaimerRequired());
    }

    // --- Composite integration tests ---

    @Test
    void shouldBuildFullPrescriptionCheckRequestWithAllNestedObjects() {
        PrescriptionCheckItem item1 = new PrescriptionCheckItem();
        item1.setDrugId("D001");
        item1.setDrugName("阿莫西林");
        item1.setDose(0.5);
        item1.setFrequency("tid");
        item1.setDuration("7d");
        item1.setRoute("口服");

        PrescriptionCheckItem item2 = new PrescriptionCheckItem();
        item2.setDrugId("D002");
        item2.setDrugName("布洛芬");
        item2.setDose(200);
        item2.setFrequency("prn");
        item2.setDuration("3d");
        item2.setRoute("口服");

        AllergyDetailItem allergy = new AllergyDetailItem();
        allergy.setAllergen("青霉素");
        allergy.setReactionType("皮疹");
        allergy.setSeverity("MODERATE");
        allergy.setOccurredAt("2025-03-01");

        List<AllergyDetailItem> allergies = new ArrayList<>();
        allergies.add(allergy);

        List<String> comorbidities = new ArrayList<>();
        comorbidities.add("高血压");

        PatientInfo patient = new PatientInfo();
        patient.setPatientId("P001");
        patient.setAge(45);
        patient.setGender("女");
        patient.setAllergyHistory("青霉素");
        patient.setAllergyDetails(allergies);
        patient.setComorbidities(comorbidities);

        PrescriptionCheckRequest request = new PrescriptionCheckRequest();
        request.setPrescriptionItems(List.of(item1, item2));
        request.setPatientInfo(patient);
        request.setPrescriptionId("RX-001");

        assertEquals(2, request.getPrescriptionItems().size());
        assertEquals("阿莫西林", request.getPrescriptionItems().get(0).getDrugName());
        assertEquals("布洛芬", request.getPrescriptionItems().get(1).getDrugName());
        assertEquals("P001", request.getPatientInfo().getPatientId());
        assertEquals(1, request.getPatientInfo().getAllergyDetails().size());
        assertEquals("青霉素", request.getPatientInfo().getAllergyDetails().get(0).getAllergen());
        assertEquals("RX-001", request.getPrescriptionId());
    }

    @Test
    void shouldBuildFullPrescriptionCheckResponseWithAllNestedObjects() {
        AlertItem alert = new AlertItem();
        alert.setAlertCode("A-001");
        alert.setAlertMessage("注意肝功能");
        alert.setSeverity("WARNING");

        DrugInteractionItem interaction = new DrugInteractionItem();
        interaction.setDrugPair("阿莫西林-华法林");
        interaction.setSeverity("CRITICAL");
        interaction.setDescription("增加出血风险");

        SuggestionItem suggestion = new SuggestionItem();
        suggestion.setSuggestionCode("S-001");
        suggestion.setSuggestionText("监测INR");

        PrescriptionCheckResponse response = new PrescriptionCheckResponse();
        response.setRiskLevel("HIGH");
        response.setAlerts(List.of(alert));
        response.setInteractions(List.of(interaction));
        response.setSuggestions(List.of(suggestion));
        response.setFromFallback(false);

        assertEquals("HIGH", response.getRiskLevel());
        assertEquals(1, response.getAlerts().size());
        assertEquals("A-001", response.getAlerts().get(0).getAlertCode());
        assertEquals(1, response.getInteractions().size());
        assertEquals("阿莫西林-华法林", response.getInteractions().get(0).getDrugPair());
        assertEquals(1, response.getSuggestions().size());
        assertEquals("S-001", response.getSuggestions().get(0).getSuggestionCode());
        assertFalse(response.isFromFallback());
    }

    @Test
    void shouldBuildFullPrescriptionAssistRequestWithAllNestedObjects() {
        ExamResultItem exam1 = new ExamResultItem();
        exam1.setItemName("WBC");
        exam1.setItemValue("12.5");
        exam1.setReferenceRange("4.0-10.0");

        ExamResultItem exam2 = new ExamResultItem();
        exam2.setItemName("CRP");
        exam2.setItemValue("50");
        exam2.setReferenceRange("<5");

        PatientInfo patient = new PatientInfo();
        patient.setPatientId("P002");
        patient.setAge(60);
        patient.setGender("男");

        PrescriptionAssistRequest request = new PrescriptionAssistRequest();
        request.setDiagnosis("细菌性肺炎");
        request.setExamResults(List.of(exam1, exam2));
        request.setPatientInfo(patient);
        request.setExistingPrescription("{}");
        request.setPrescriptionId("RX-002");
        request.setEncounterId("ENC-002");

        assertEquals("细菌性肺炎", request.getDiagnosis());
        assertEquals(2, request.getExamResults().size());
        assertEquals("WBC", request.getExamResults().get(0).getItemName());
        assertEquals("CRP", request.getExamResults().get(1).getItemName());
        assertEquals("P002", request.getPatientInfo().getPatientId());
        assertEquals("{}", request.getExistingPrescription());
        assertEquals("RX-002", request.getPrescriptionId());
        assertEquals("ENC-002", request.getEncounterId());
    }

    @Test
    void shouldBuildFullPrescriptionAssistResponseWithAllNestedObjects() {
        DoseWarningItem doseWarning = new DoseWarningItem();
        doseWarning.setDrugId("D001");
        doseWarning.setWarningType("OVER_DAILY_DOSE");
        doseWarning.setMessage("日剂量超过安全上限");
        doseWarning.setSeverity("WARNING");

        AllergyWarningItem allergyWarning = new AllergyWarningItem();
        allergyWarning.setDrugId("D002");
        allergyWarning.setAllergen("头孢");
        allergyWarning.setSeverity("HIGH");

        PrescriptionAssistResponse response = new PrescriptionAssistResponse();
        response.setPrescriptionDraft("{\"medications\":[]}");
        response.setDoseWarnings(List.of(doseWarning));
        response.setAllergyWarnings(List.of(allergyWarning));
        response.setErrorCode(null);
        response.setDisclaimerRequired(true);

        assertEquals("{\"medications\":[]}", response.getPrescriptionDraft());
        assertEquals(1, response.getDoseWarnings().size());
        assertEquals("OVER_DAILY_DOSE", response.getDoseWarnings().get(0).getWarningType());
        assertEquals(1, response.getAllergyWarnings().size());
        assertEquals("头孢", response.getAllergyWarnings().get(0).getAllergen());
        assertNull(response.getErrorCode());
        assertTrue(response.isDisclaimerRequired());
    }
}
