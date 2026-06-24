package com.aimedical.modules.patient.converter;

import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.patient.dto.*;
import com.aimedical.modules.patient.entity.*;

import java.util.List;
import java.util.stream.Collectors;

public class PatientConverter {

    public static PatientDto toDto(PatientEntity entity) {
        if (entity == null) return null;
        PatientDto dto = new PatientDto();
        dto.setId(entity.getId());
        if (entity.getUser() != null) {
            User user = entity.getUser();
            dto.setUserId(user.getId());
            dto.setName(user.getNickname());
            dto.setPhone(user.getPhone());
            dto.setGender(user.getGender());
            dto.setAge(user.getAge());
            dto.setEmail(user.getEmail());
        }
        dto.setEmergencyContact(entity.getEmergencyContact());
        return dto;
    }

    // === Allergy ===
    public static AllergyResponse toAllergyResponse(PatientAllergy entity) {
        AllergyResponse r = new AllergyResponse();
        r.setId(entity.getId());
        r.setAllergen(entity.getAllergen());
        r.setReactionType(entity.getReactionType());
        r.setSeverity(entity.getSeverity());
        r.setOccurredAt(entity.getOccurredAt());
        return r;
    }

    public static PatientAllergy toAllergyEntity(AllergyRequest req, PatientEntity patient) {
        PatientAllergy e = new PatientAllergy();
        e.setPatient(patient);
        e.setAllergen(req.getAllergen());
        e.setReactionType(req.getReactionType());
        e.setSeverity(req.getSeverity());
        e.setOccurredAt(req.getOccurredAt());
        return e;
    }

    // === Chronic Disease ===
    public static ChronicDiseaseResponse toChronicResponse(PatientChronicDisease entity) {
        ChronicDiseaseResponse r = new ChronicDiseaseResponse();
        r.setId(entity.getId());
        r.setDiseaseName(entity.getDiseaseName());
        r.setDiagnosedAt(entity.getDiagnosedAt());
        r.setCurrentStatus(entity.getCurrentStatus());
        return r;
    }

    public static PatientChronicDisease toChronicEntity(ChronicDiseaseRequest req, PatientEntity patient) {
        PatientChronicDisease e = new PatientChronicDisease();
        e.setPatient(patient);
        e.setDiseaseName(req.getDiseaseName());
        e.setDiagnosedAt(req.getDiagnosedAt());
        e.setCurrentStatus(req.getCurrentStatus());
        return e;
    }

    // === Family History ===
    public static FamilyHistoryResponse toFamilyResponse(PatientFamilyHistory entity) {
        FamilyHistoryResponse r = new FamilyHistoryResponse();
        r.setId(entity.getId());
        r.setRelationship(entity.getRelationship());
        r.setDiseaseName(entity.getDiseaseName());
        r.setNote(entity.getNote());
        return r;
    }

    public static PatientFamilyHistory toFamilyEntity(FamilyHistoryRequest req, PatientEntity patient) {
        PatientFamilyHistory e = new PatientFamilyHistory();
        e.setPatient(patient);
        e.setRelationship(req.getRelationship());
        e.setDiseaseName(req.getDiseaseName());
        e.setNote(req.getNote());
        return e;
    }

    // === Surgery ===
    public static SurgeryHistoryResponse toSurgeryResponse(PatientSurgeryHistory entity) {
        SurgeryHistoryResponse r = new SurgeryHistoryResponse();
        r.setId(entity.getId());
        r.setSurgeryName(entity.getSurgeryName());
        r.setSurgeryAt(entity.getSurgeryAt());
        r.setHospital(entity.getHospital());
        return r;
    }

    public static PatientSurgeryHistory toSurgeryEntity(SurgeryHistoryRequest req, PatientEntity patient) {
        PatientSurgeryHistory e = new PatientSurgeryHistory();
        e.setPatient(patient);
        e.setSurgeryName(req.getSurgeryName());
        e.setSurgeryAt(req.getSurgeryAt());
        e.setHospital(req.getHospital());
        return e;
    }

    // === Medication ===
    public static MedicationHistoryResponse toMedicationResponse(PatientMedicationHistory entity) {
        MedicationHistoryResponse r = new MedicationHistoryResponse();
        r.setId(entity.getId());
        r.setDrugName(entity.getDrugName());
        r.setReason(entity.getReason());
        r.setStartedAt(entity.getStartedAt());
        r.setEndedAt(entity.getEndedAt());
        return r;
    }

    public static PatientMedicationHistory toMedicationEntity(MedicationHistoryRequest req, PatientEntity patient) {
        PatientMedicationHistory e = new PatientMedicationHistory();
        e.setPatient(patient);
        e.setDrugName(req.getDrugName());
        e.setReason(req.getReason());
        e.setStartedAt(req.getStartedAt());
        e.setEndedAt(req.getEndedAt());
        return e;
    }

    // === Summary ===
    public static HealthRecordSummaryResponse toHealthRecordSummary(PatientEntity entity) {
        HealthRecordSummaryResponse r = new HealthRecordSummaryResponse();
        if (entity.getAllergies() != null) {
            r.setAllergies(entity.getAllergies().stream().map(PatientConverter::toAllergyResponse).collect(Collectors.toList()));
        }
        if (entity.getChronicDiseases() != null) {
            r.setChronicDiseases(entity.getChronicDiseases().stream().map(PatientConverter::toChronicResponse).collect(Collectors.toList()));
        }
        if (entity.getFamilyHistories() != null) {
            r.setFamilyHistories(entity.getFamilyHistories().stream().map(PatientConverter::toFamilyResponse).collect(Collectors.toList()));
        }
        if (entity.getSurgeryHistories() != null) {
            r.setSurgeryHistories(entity.getSurgeryHistories().stream().map(PatientConverter::toSurgeryResponse).collect(Collectors.toList()));
        }
        if (entity.getMedicationHistories() != null) {
            r.setMedicationHistories(entity.getMedicationHistories().stream().map(PatientConverter::toMedicationResponse).collect(Collectors.toList()));
        }
        return r;
    }
}
