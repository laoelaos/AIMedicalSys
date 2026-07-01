package com.aimedical.modules.ai.api.dto.medicalrecord;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordDtoTest {

    // --- MedicalRecordGenRequest ---

    @Test
    void shouldCreateRequestWithDefaultConstructor() {
        MedicalRecordGenRequest request = new MedicalRecordGenRequest();
        assertNull(request.getDialogueText());
        assertNull(request.getPatientId());
        assertNull(request.getEncounterId());
        assertFalse(request.isStream());
        assertNull(request.getDepartmentId());
    }

    @Test
    void shouldSetAndGetDialogueText() {
        MedicalRecordGenRequest request = new MedicalRecordGenRequest();
        request.setDialogueText("医患对话内容");
        assertEquals("医患对话内容", request.getDialogueText());
    }

    @Test
    void shouldSetAndGetPatientId() {
        MedicalRecordGenRequest request = new MedicalRecordGenRequest();
        request.setPatientId("P001");
        assertEquals("P001", request.getPatientId());
    }

    @Test
    void shouldSetAndGetEncounterId() {
        MedicalRecordGenRequest request = new MedicalRecordGenRequest();
        request.setEncounterId("ENC-001");
        assertEquals("ENC-001", request.getEncounterId());
    }

    @Test
    void shouldDefaultStreamToFalse() {
        MedicalRecordGenRequest request = new MedicalRecordGenRequest();
        assertFalse(request.isStream());
    }

    @Test
    void shouldSetAndGetStream() {
        MedicalRecordGenRequest request = new MedicalRecordGenRequest();
        request.setStream(true);
        assertTrue(request.isStream());
    }

    @Test
    void shouldSetAndGetDepartmentId() {
        MedicalRecordGenRequest request = new MedicalRecordGenRequest();
        request.setDepartmentId("dept-01");
        assertEquals("dept-01", request.getDepartmentId());
    }

    @Test
    void shouldBuildFullRequestWithAllFields() {
        MedicalRecordGenRequest request = new MedicalRecordGenRequest();
        request.setDialogueText("患者：头痛三天\n医生：有无发烧？");
        request.setPatientId("P001");
        request.setEncounterId("ENC-001");
        request.setStream(true);
        request.setDepartmentId("dept-01");

        assertEquals("患者：头痛三天\n医生：有无发烧？", request.getDialogueText());
        assertEquals("P001", request.getPatientId());
        assertEquals("ENC-001", request.getEncounterId());
        assertTrue(request.isStream());
        assertEquals("dept-01", request.getDepartmentId());
    }

    @Test
    void shouldAllowStreamToggleBackToFalse() {
        MedicalRecordGenRequest request = new MedicalRecordGenRequest();
        request.setStream(true);
        assertTrue(request.isStream());
        request.setStream(false);
        assertFalse(request.isStream());
    }

    // --- MedicalRecordGenResponse ---

    @Test
    void shouldCreateResponseWithDefaultConstructor() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        assertNull(response.getChiefComplaint());
        assertNull(response.getSymptomDescription());
        assertNull(response.getPresentIllness());
        assertNull(response.getPastHistory());
        assertNull(response.getPhysicalExam());
        assertNull(response.getPreliminaryDiagnosis());
        assertNull(response.getTreatmentPlan());
        assertNull(response.getMissingFields());
        assertNull(response.getPartialContent());
    }

    @Test
    void shouldSetAndGetChiefComplaint() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setChiefComplaint("头痛三天");
        assertEquals("头痛三天", response.getChiefComplaint());
    }

    @Test
    void shouldSetAndGetSymptomDescription() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setSymptomDescription("头痛，恶心，畏光");
        assertEquals("头痛，恶心，畏光", response.getSymptomDescription());
    }

    @Test
    void shouldSetAndGetPresentIllness() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setPresentIllness("患者3天前无明显诱因出现头痛");
        assertEquals("患者3天前无明显诱因出现头痛", response.getPresentIllness());
    }

    @Test
    void shouldSetAndGetPastHistory() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setPastHistory("既往体健，否认高血压、糖尿病史");
        assertEquals("既往体健，否认高血压、糖尿病史", response.getPastHistory());
    }

    @Test
    void shouldSetAndGetPhysicalExam() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setPhysicalExam("T 36.5℃，P 80次/分，R 20次/分，BP 120/80mmHg");
        assertEquals("T 36.5℃，P 80次/分，R 20次/分，BP 120/80mmHg", response.getPhysicalExam());
    }

    @Test
    void shouldSetAndGetPreliminaryDiagnosis() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setPreliminaryDiagnosis("偏头痛");
        assertEquals("偏头痛", response.getPreliminaryDiagnosis());
    }

    @Test
    void shouldSetAndGetTreatmentPlan() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setTreatmentPlan("布洛芬 200mg 口服");
        assertEquals("布洛芬 200mg 口服", response.getTreatmentPlan());
    }

    @Test
    void shouldSetAndGetMissingFields() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        List<String> missing = new ArrayList<>();
        missing.add("既往史");
        missing.add("体格检查");
        response.setMissingFields(missing);
        assertEquals(2, response.getMissingFields().size());
        assertEquals("既往史", response.getMissingFields().get(0));
        assertEquals("体格检查", response.getMissingFields().get(1));
    }

    @Test
    void shouldDefaultMissingFieldsToNull() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        assertNull(response.getMissingFields());
    }

    @Test
    void shouldSetAndGetPartialContentAsString() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setPartialContent("部分内容");
        assertEquals("部分内容", response.getPartialContent());
    }

    @Test
    void shouldSetAndGetPartialContentAsObject() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        Object content = new Object();
        response.setPartialContent(content);
        assertSame(content, response.getPartialContent());
    }

    @Test
    void shouldDefaultPartialContentToNull() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        assertNull(response.getPartialContent());
    }

    @Test
    void shouldBuildFullResponseWithAllFields() {
        List<String> missingFields = new ArrayList<>();
        missingFields.add("过敏史");
        missingFields.add("家族史");

        Object partialData = new Object();

        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setChiefComplaint("腹痛2天");
        response.setSymptomDescription("上腹部疼痛，呈持续性");
        response.setPresentIllness("患者2天前开始出现上腹部疼痛");
        response.setPastHistory("慢性胃炎史5年");
        response.setPhysicalExam("腹软，上腹部压痛");
        response.setPreliminaryDiagnosis("慢性胃炎急性发作");
        response.setTreatmentPlan("奥美拉唑 20mg qd");
        response.setMissingFields(missingFields);
        response.setPartialContent(partialData);

        assertEquals("腹痛2天", response.getChiefComplaint());
        assertEquals("上腹部疼痛，呈持续性", response.getSymptomDescription());
        assertEquals("患者2天前开始出现上腹部疼痛", response.getPresentIllness());
        assertEquals("慢性胃炎史5年", response.getPastHistory());
        assertEquals("腹软，上腹部压痛", response.getPhysicalExam());
        assertEquals("慢性胃炎急性发作", response.getPreliminaryDiagnosis());
        assertEquals("奥美拉唑 20mg qd", response.getTreatmentPlan());
        assertEquals(2, response.getMissingFields().size());
        assertEquals("过敏史", response.getMissingFields().get(0));
        assertEquals("家族史", response.getMissingFields().get(1));
        assertSame(partialData, response.getPartialContent());
    }
}
