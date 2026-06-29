package com.aimedical.modules.patient.converter;

import com.aimedical.modules.patient.dto.*;
import com.aimedical.modules.patient.entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatientConverterTest {

    @Test
    void toDtoShouldReturnNullForNullEntity() {
        assertNull(PatientConverter.toDto(null, null, null));
    }

    @Test
    void toDtoShouldMapAllFields() {
        PatientEntity entity = new PatientEntity();
        entity.setId(1L);
        entity.setUserId(10L);
        entity.setRealName("张三");
        entity.setPhone("13800000000");
        entity.setGender(Gender.MALE);
        entity.setEmergencyContact("李四-13800000001");
        entity.setAvatarUrl("http://avatar.url");

        PatientDto dto = PatientConverter.toDto(entity, "test@example.com", 30);

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getUserId());
        assertEquals("张三", dto.getName());
        assertEquals("13800000000", dto.getPhone());
        assertEquals("男", dto.getGender());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals(30, dto.getAge());
        assertEquals("李四-13800000001", dto.getEmergencyContact());
        assertEquals("http://avatar.url", dto.getAvatarUrl());
    }

    @Test
    void toDtoShouldHandleNullGender() {
        PatientEntity entity = new PatientEntity();
        entity.setUserId(1L);
        entity.setRealName("test");
        PatientDto dto = PatientConverter.toDto(entity, null, null);
        assertNull(dto.getGender());
    }

    @Test
    void toAllergyResponseShouldMapAllFields() {
        PatientAllergy entity = new PatientAllergy();
        entity.setId(1L);
        entity.setAllergen("花粉");
        entity.setReactionType("打喷嚏");
        entity.setSeverity(AllergySeverity.MILD);
        entity.setOccurredAt(LocalDate.of(2024, 6, 15));

        AllergyResponse r = PatientConverter.toAllergyResponse(entity);
        assertEquals(1L, r.getId());
        assertEquals("花粉", r.getAllergen());
        assertEquals("打喷嚏", r.getReactionType());
        assertEquals("MILD", r.getSeverity());
        assertEquals("2024-06-15", r.getOccurredAt());
    }

    @Test
    void toAllergyResponseShouldHandleNulls() {
        PatientAllergy entity = new PatientAllergy();
        entity.setId(1L);
        entity.setAllergen("青霉素");

        AllergyResponse r = PatientConverter.toAllergyResponse(entity);
        assertNull(r.getReactionType());
        assertNull(r.getSeverity());
        assertNull(r.getOccurredAt());
    }

    @Test
    void toAllergyEntityShouldMapRequest() {
        AllergyRequest req = new AllergyRequest();
        req.setAllergen("花生");
        req.setReactionType("皮疹");
        req.setSeverity("SEVERE");
        req.setOccurredAt("2024-01-01");

        PatientEntity patient = new PatientEntity();
        PatientAllergy entity = PatientConverter.toAllergyEntity(req, patient);

        assertSame(patient, entity.getPatient());
        assertEquals("花生", entity.getAllergen());
        assertEquals("皮疹", entity.getReactionType());
        assertEquals(AllergySeverity.SEVERE, entity.getSeverity());
        assertEquals(LocalDate.of(2024, 1, 1), entity.getOccurredAt());
    }

    @Test
    void toAllergyEntityShouldHandleNullSeverityAndDate() {
        AllergyRequest req = new AllergyRequest();
        req.setAllergen("尘螨");

        PatientAllergy entity = PatientConverter.toAllergyEntity(req, new PatientEntity());
        assertEquals("尘螨", entity.getAllergen());
        assertNull(entity.getSeverity());
        assertNull(entity.getOccurredAt());
    }

    @Test
    void toChronicResponseShouldMapAllFields() {
        PatientChronicDisease entity = new PatientChronicDisease();
        entity.setId(1L);
        entity.setDiseaseName("高血压");
        entity.setDiagnosedAt(LocalDate.of(2023, 1, 1));
        entity.setCurrentStatus(DiseaseStatus.STABLE);

        ChronicDiseaseResponse r = PatientConverter.toChronicResponse(entity);
        assertEquals(1L, r.getId());
        assertEquals("高血压", r.getDiseaseName());
        assertEquals("2023-01-01", r.getDiagnosedAt());
        assertEquals("STABLE", r.getCurrentStatus());
    }

    @Test
    void toChronicResponseShouldHandleNulls() {
        PatientChronicDisease entity = new PatientChronicDisease();
        entity.setId(1L);
        entity.setDiseaseName("糖尿病");

        ChronicDiseaseResponse r = PatientConverter.toChronicResponse(entity);
        assertNull(r.getDiagnosedAt());
        assertNull(r.getCurrentStatus());
    }

    @Test
    void toChronicEntityShouldMapRequest() {
        ChronicDiseaseRequest req = new ChronicDiseaseRequest();
        req.setDiseaseName("糖尿病");
        req.setDiagnosedAt("2022-06-15");
        req.setCurrentStatus("UNSTABLE");

        PatientChronicDisease entity = PatientConverter.toChronicEntity(req, new PatientEntity());
        assertEquals("糖尿病", entity.getDiseaseName());
        assertEquals(LocalDate.of(2022, 6, 15), entity.getDiagnosedAt());
        assertEquals(DiseaseStatus.UNSTABLE, entity.getCurrentStatus());
    }

    @Test
    void toChronicEntityShouldHandleNulls() {
        ChronicDiseaseRequest req = new ChronicDiseaseRequest();
        req.setDiseaseName("高血压");

        PatientChronicDisease entity = PatientConverter.toChronicEntity(req, new PatientEntity());
        assertNull(entity.getDiagnosedAt());
        assertNull(entity.getCurrentStatus());
    }

    @Test
    void toFamilyResponseShouldMapAllFields() {
        PatientFamilyHistory entity = new PatientFamilyHistory();
        entity.setId(1L);
        entity.setRelationship("父亲");
        entity.setDiseaseName("高血压");
        entity.setNote("确诊于2020年");

        FamilyHistoryResponse r = PatientConverter.toFamilyResponse(entity);
        assertEquals(1L, r.getId());
        assertEquals("父亲", r.getRelationship());
        assertEquals("高血压", r.getDiseaseName());
        assertEquals("确诊于2020年", r.getNote());
    }

    @Test
    void toFamilyEntityShouldMapRequest() {
        FamilyHistoryRequest req = new FamilyHistoryRequest();
        req.setRelationship("母亲");
        req.setDiseaseName("糖尿病");
        req.setNote("需注意饮食");

        PatientFamilyHistory entity = PatientConverter.toFamilyEntity(req, new PatientEntity());
        assertEquals("母亲", entity.getRelationship());
        assertEquals("糖尿病", entity.getDiseaseName());
        assertEquals("需注意饮食", entity.getNote());
    }

    @Test
    void toSurgeryResponseShouldMapAllFields() {
        PatientSurgeryHistory entity = new PatientSurgeryHistory();
        entity.setId(1L);
        entity.setSurgeryName("阑尾切除术");
        entity.setSurgeryAt(LocalDate.of(2023, 5, 10));
        entity.setHospital("市人民医院");

        SurgeryHistoryResponse r = PatientConverter.toSurgeryResponse(entity);
        assertEquals(1L, r.getId());
        assertEquals("阑尾切除术", r.getSurgeryName());
        assertEquals("2023-05-10", r.getSurgeryAt());
        assertEquals("市人民医院", r.getHospital());
    }

    @Test
    void toSurgeryResponseShouldHandleNullDate() {
        PatientSurgeryHistory entity = new PatientSurgeryHistory();
        entity.setId(1L);
        entity.setSurgeryName("手术");

        SurgeryHistoryResponse r = PatientConverter.toSurgeryResponse(entity);
        assertNull(r.getSurgeryAt());
    }

    @Test
    void toSurgeryEntityShouldMapRequest() {
        SurgeryHistoryRequest req = new SurgeryHistoryRequest();
        req.setSurgeryName("胆囊切除术");
        req.setSurgeryAt("2022-11-20");
        req.setHospital("省立医院");

        PatientSurgeryHistory entity = PatientConverter.toSurgeryEntity(req, new PatientEntity());
        assertEquals("胆囊切除术", entity.getSurgeryName());
        assertEquals(LocalDate.of(2022, 11, 20), entity.getSurgeryAt());
        assertEquals("省立医院", entity.getHospital());
    }

    @Test
    void toMedicationResponseShouldMapAllFields() {
        PatientMedicationHistory entity = new PatientMedicationHistory();
        entity.setId(1L);
        entity.setDrugName("阿莫西林");
        entity.setReason("呼吸道感染");
        entity.setStartedAt(LocalDate.of(2024, 1, 1));
        entity.setEndedAt(LocalDate.of(2024, 1, 14));

        MedicationHistoryResponse r = PatientConverter.toMedicationResponse(entity);
        assertEquals(1L, r.getId());
        assertEquals("阿莫西林", r.getDrugName());
        assertEquals("呼吸道感染", r.getReason());
        assertEquals("2024-01-01", r.getStartedAt());
        assertEquals("2024-01-14", r.getEndedAt());
    }

    @Test
    void toMedicationResponseShouldHandleNulls() {
        PatientMedicationHistory entity = new PatientMedicationHistory();
        entity.setId(1L);
        entity.setDrugName("布洛芬");

        MedicationHistoryResponse r = PatientConverter.toMedicationResponse(entity);
        assertNull(r.getReason());
        assertNull(r.getStartedAt());
        assertNull(r.getEndedAt());
    }

    @Test
    void toMedicationEntityShouldMapRequest() {
        MedicationHistoryRequest req = new MedicationHistoryRequest();
        req.setDrugName("布洛芬");
        req.setReason("退烧");
        req.setStartedAt("2024-03-01");
        req.setEndedAt("2024-03-07");

        PatientMedicationHistory entity = PatientConverter.toMedicationEntity(req, new PatientEntity());
        assertEquals("布洛芬", entity.getDrugName());
        assertEquals("退烧", entity.getReason());
        assertEquals(LocalDate.of(2024, 3, 1), entity.getStartedAt());
        assertEquals(LocalDate.of(2024, 3, 7), entity.getEndedAt());
    }

    @Test
    void toHealthRecordSummaryShouldMapAllCollections() {
        PatientEntity entity = new PatientEntity();

        PatientAllergy allergy = new PatientAllergy();
        allergy.setId(1L);
        allergy.setAllergen("花粉");
        entity.setAllergies(List.of(allergy));

        PatientChronicDisease cd = new PatientChronicDisease();
        cd.setId(1L);
        cd.setDiseaseName("高血压");
        entity.setChronicDiseases(List.of(cd));

        PatientFamilyHistory fh = new PatientFamilyHistory();
        fh.setId(1L);
        fh.setRelationship("父亲");
        entity.setFamilyHistories(List.of(fh));

        PatientSurgeryHistory sh = new PatientSurgeryHistory();
        sh.setId(1L);
        sh.setSurgeryName("手术");
        entity.setSurgeryHistories(List.of(sh));

        PatientMedicationHistory mh = new PatientMedicationHistory();
        mh.setId(1L);
        mh.setDrugName("阿莫西林");
        entity.setMedicationHistories(List.of(mh));

        HealthRecordSummaryResponse r = PatientConverter.toHealthRecordSummary(entity);
        assertEquals(1, r.getAllergies().size());
        assertEquals(1, r.getChronicDiseases().size());
        assertEquals(1, r.getFamilyHistories().size());
        assertEquals(1, r.getSurgeryHistories().size());
        assertEquals(1, r.getMedicationHistories().size());
    }

    @Test
    void toHealthRecordSummaryShouldHandleNullCollections() {
        PatientEntity entity = new PatientEntity();
        HealthRecordSummaryResponse r = PatientConverter.toHealthRecordSummary(entity);
        assertTrue(r.getAllergies().isEmpty());
        assertTrue(r.getChronicDiseases().isEmpty());
        assertTrue(r.getFamilyHistories().isEmpty());
        assertTrue(r.getSurgeryHistories().isEmpty());
        assertTrue(r.getMedicationHistories().isEmpty());
    }

    @Test
    void mergeFromRequestShouldApplyNonNullFields() {
        PatientEntity patient = new PatientEntity();
        patient.setRealName("原姓名");
        patient.setPhone("13800000000");
        patient.setGender(Gender.FEMALE);

        PatientProfileUpdateRequest req = new PatientProfileUpdateRequest();
        req.setName("新姓名");
        req.setEmergencyContact("新联系人");

        PatientConverter.mergeFromRequest(patient, req);
        assertEquals("新姓名", patient.getRealName());
        assertEquals("13800000000", patient.getPhone());
        assertEquals(Gender.FEMALE, patient.getGender());
        assertEquals("新联系人", patient.getEmergencyContact());
    }

    @Test
    void mergeFromRequestShouldUpdateGender() {
        PatientEntity patient = new PatientEntity();
        patient.setGender(Gender.FEMALE);

        PatientProfileUpdateRequest req = new PatientProfileUpdateRequest();
        req.setGender("男");

        PatientConverter.mergeFromRequest(patient, req);
        assertEquals(Gender.MALE, patient.getGender());
    }

    @Test
    void mergeFromRequestShouldIgnoreNullFields() {
        PatientEntity patient = new PatientEntity();
        patient.setRealName("原姓名");

        PatientProfileUpdateRequest req = new PatientProfileUpdateRequest();
        PatientConverter.mergeFromRequest(patient, req);
        assertEquals("原姓名", patient.getRealName());
    }
}
