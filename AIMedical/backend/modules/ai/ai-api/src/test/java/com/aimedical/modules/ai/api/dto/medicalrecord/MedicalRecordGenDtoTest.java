package com.aimedical.modules.ai.api.dto.medicalrecord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordGenDtoTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        assertNull(response.getChiefComplaint());
        assertNull(response.getPresentIllness());
        assertNull(response.getPastHistory());
        assertNull(response.getPreliminaryDiagnosis());
        assertNull(response.getTreatmentPlan());
    }

    @Test
    void shouldSetAndGetChiefComplaint() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setChiefComplaint("头痛伴恶心三天");
        assertEquals("头痛伴恶心三天", response.getChiefComplaint());
    }

    @Test
    void shouldSetAndGetPresentIllness() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setPresentIllness("患者三天前无明显诱因出现头痛");
        assertEquals("患者三天前无明显诱因出现头痛", response.getPresentIllness());
    }

    @Test
    void shouldSetAndGetPastHistory() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setPastHistory("高血压病史五年");
        assertEquals("高血压病史五年", response.getPastHistory());
    }

    @Test
    void shouldSetAndGetDiagnosis() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setPreliminaryDiagnosis("偏头痛");
        assertEquals("偏头痛", response.getPreliminaryDiagnosis());
    }

    @Test
    void shouldSetAndGetTreatmentPlan() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setTreatmentPlan("布洛芬口服，注意休息");
        assertEquals("布洛芬口服，注意休息", response.getTreatmentPlan());
    }

    @Test
    void shouldBuildFullMedicalRecord() {
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        response.setChiefComplaint("头痛三天");
        response.setPresentIllness("三天前无明显诱因出现头痛");
        response.setPastHistory("高血压五年");
        response.setPreliminaryDiagnosis("偏头痛");
        response.setTreatmentPlan("布洛芬口服");

        assertEquals("头痛三天", response.getChiefComplaint());
        assertEquals("三天前无明显诱因出现头痛", response.getPresentIllness());
        assertEquals("高血压五年", response.getPastHistory());
        assertEquals("偏头痛", response.getPreliminaryDiagnosis());
        assertEquals("布洛芬口服", response.getTreatmentPlan());
    }
}
