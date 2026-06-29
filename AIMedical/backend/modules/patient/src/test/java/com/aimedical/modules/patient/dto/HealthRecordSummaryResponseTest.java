package com.aimedical.modules.patient.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HealthRecordSummaryResponseTest {

    @Test
    void shouldSetAndGetAllergies() {
        HealthRecordSummaryResponse r = new HealthRecordSummaryResponse();
        AllergyResponse a = new AllergyResponse();
        a.setAllergen("花粉");
        r.setAllergies(List.of(a));
        assertEquals(1, r.getAllergies().size());
        assertEquals("花粉", r.getAllergies().get(0).getAllergen());
    }

    @Test
    void shouldSetAndGetChronicDiseases() {
        HealthRecordSummaryResponse r = new HealthRecordSummaryResponse();
        ChronicDiseaseResponse c = new ChronicDiseaseResponse();
        c.setDiseaseName("高血压");
        r.setChronicDiseases(List.of(c));
        assertEquals(1, r.getChronicDiseases().size());
        assertEquals("高血压", r.getChronicDiseases().get(0).getDiseaseName());
    }

    @Test
    void shouldSetAndGetFamilyHistories() {
        HealthRecordSummaryResponse r = new HealthRecordSummaryResponse();
        FamilyHistoryResponse f = new FamilyHistoryResponse();
        f.setRelationship("父亲");
        r.setFamilyHistories(List.of(f));
        assertEquals(1, r.getFamilyHistories().size());
        assertEquals("父亲", r.getFamilyHistories().get(0).getRelationship());
    }

    @Test
    void shouldSetAndGetSurgeryHistories() {
        HealthRecordSummaryResponse r = new HealthRecordSummaryResponse();
        SurgeryHistoryResponse s = new SurgeryHistoryResponse();
        s.setSurgeryName("阑尾切除");
        r.setSurgeryHistories(List.of(s));
        assertEquals(1, r.getSurgeryHistories().size());
        assertEquals("阑尾切除", r.getSurgeryHistories().get(0).getSurgeryName());
    }

    @Test
    void shouldSetAndGetMedicationHistories() {
        HealthRecordSummaryResponse r = new HealthRecordSummaryResponse();
        MedicationHistoryResponse m = new MedicationHistoryResponse();
        m.setDrugName("阿莫西林");
        r.setMedicationHistories(List.of(m));
        assertEquals(1, r.getMedicationHistories().size());
        assertEquals("阿莫西林", r.getMedicationHistories().get(0).getDrugName());
    }
}
