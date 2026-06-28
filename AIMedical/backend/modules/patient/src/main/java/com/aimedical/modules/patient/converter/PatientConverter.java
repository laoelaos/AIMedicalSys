package com.aimedical.modules.patient.converter;

import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.patient.dto.*;
import com.aimedical.modules.patient.entity.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PatientConverter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private PatientConverter() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static PatientDto toDto(PatientEntity entity) {
        if (entity == null) return null;
        PatientDto dto = new PatientDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setName(entity.getRealName());
        dto.setPhone(entity.getPhone());
        dto.setGender(entity.getGender() != null ? entity.getGender().getDesc() : null);
        dto.setEmergencyContact(entity.getEmergencyContact());
        // Supplement with User data when available for fields not on PatientEntity
        if (entity.getUser() != null) {
            User user = entity.getUser();
            dto.setAge(user.getAge());
            dto.setEmail(user.getEmail());
        }
        return dto;
    }

    // === Allergy ===
    public static AllergyResponse toAllergyResponse(PatientAllergy entity) {
        AllergyResponse r = new AllergyResponse();
        r.setId(entity.getId());
        r.setAllergen(entity.getAllergen());
        r.setReactionType(entity.getReactionType());
        r.setSeverity(entity.getSeverity());
        r.setOccurredAt(entity.getOccurredAt() != null ? entity.getOccurredAt().format(DATE_FMT) : null);
        return r;
    }

    public static PatientAllergy toAllergyEntity(AllergyRequest req, PatientEntity patient) {
        PatientAllergy e = new PatientAllergy();
        e.setPatient(patient);
        e.setAllergen(req.getAllergen());
        e.setReactionType(req.getReactionType());
        e.setSeverity(req.getSeverity());
        e.setOccurredAt(parseDate(req.getOccurredAt()));
        return e;
    }

    // === Chronic Disease ===
    public static ChronicDiseaseResponse toChronicResponse(PatientChronicDisease entity) {
        ChronicDiseaseResponse r = new ChronicDiseaseResponse();
        r.setId(entity.getId());
        r.setDiseaseName(entity.getDiseaseName());
        r.setDiagnosedAt(entity.getDiagnosedAt() != null ? entity.getDiagnosedAt().format(DATE_FMT) : null);
        r.setCurrentStatus(entity.getCurrentStatus());
        return r;
    }

    public static PatientChronicDisease toChronicEntity(ChronicDiseaseRequest req, PatientEntity patient) {
        PatientChronicDisease e = new PatientChronicDisease();
        e.setPatient(patient);
        e.setDiseaseName(req.getDiseaseName());
        e.setDiagnosedAt(parseDate(req.getDiagnosedAt()));
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
        r.setSurgeryAt(entity.getSurgeryAt() != null ? entity.getSurgeryAt().format(DATE_FMT) : null);
        r.setHospital(entity.getHospital());
        return r;
    }

    public static PatientSurgeryHistory toSurgeryEntity(SurgeryHistoryRequest req, PatientEntity patient) {
        PatientSurgeryHistory e = new PatientSurgeryHistory();
        e.setPatient(patient);
        e.setSurgeryName(req.getSurgeryName());
        e.setSurgeryAt(parseDate(req.getSurgeryAt()));
        e.setHospital(req.getHospital());
        return e;
    }

    // === Medication ===
    public static MedicationHistoryResponse toMedicationResponse(PatientMedicationHistory entity) {
        MedicationHistoryResponse r = new MedicationHistoryResponse();
        r.setId(entity.getId());
        r.setDrugName(entity.getDrugName());
        r.setReason(entity.getReason());
        r.setStartedAt(entity.getStartedAt() != null ? entity.getStartedAt().format(DATE_FMT) : null);
        r.setEndedAt(entity.getEndedAt() != null ? entity.getEndedAt().format(DATE_FMT) : null);
        return r;
    }

    public static PatientMedicationHistory toMedicationEntity(MedicationHistoryRequest req, PatientEntity patient) {
        PatientMedicationHistory e = new PatientMedicationHistory();
        e.setPatient(patient);
        e.setDrugName(req.getDrugName());
        e.setReason(req.getReason());
        e.setStartedAt(parseDate(req.getStartedAt()));
        e.setEndedAt(parseDate(req.getEndedAt()));
        return e;
    }

    // === Summary ===
    public static HealthRecordSummaryResponse toHealthRecordSummary(PatientEntity entity) {
        HealthRecordSummaryResponse r = new HealthRecordSummaryResponse();
        r.setAllergies(entity.getAllergies() != null
                ? entity.getAllergies().stream().map(PatientConverter::toAllergyResponse).collect(Collectors.toList())
                : Collections.emptyList());
        r.setChronicDiseases(entity.getChronicDiseases() != null
                ? entity.getChronicDiseases().stream().map(PatientConverter::toChronicResponse).collect(Collectors.toList())
                : Collections.emptyList());
        r.setFamilyHistories(entity.getFamilyHistories() != null
                ? entity.getFamilyHistories().stream().map(PatientConverter::toFamilyResponse).collect(Collectors.toList())
                : Collections.emptyList());
        r.setSurgeryHistories(entity.getSurgeryHistories() != null
                ? entity.getSurgeryHistories().stream().map(PatientConverter::toSurgeryResponse).collect(Collectors.toList())
                : Collections.emptyList());
        r.setMedicationHistories(entity.getMedicationHistories() != null
                ? entity.getMedicationHistories().stream().map(PatientConverter::toMedicationResponse).collect(Collectors.toList())
                : Collections.emptyList());
        return r;
    }

    /**
     * Apply non-null fields from request onto existing PatientEntity (T15).
     */
    public static void mergeFromRequest(PatientEntity patient, PatientProfileUpdateRequest request) {
        if (request.getName() != null) patient.setRealName(request.getName());
        if (request.getPhone() != null) patient.setPhone(request.getPhone());
        if (request.getGender() != null) {
            patient.setGender(Gender.fromLabel(request.getGender()));
        }
        if (request.getEmergencyContact() != null) patient.setEmergencyContact(request.getEmergencyContact());
    }

    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr, DATE_FMT);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }
}
