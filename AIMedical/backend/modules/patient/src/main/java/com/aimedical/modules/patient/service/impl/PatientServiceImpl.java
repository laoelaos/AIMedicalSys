package com.aimedical.modules.patient.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.patient.exception.PatientErrorCode;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.commonmodule.api.dto.LoginRequest;
import com.aimedical.modules.commonmodule.api.dto.RegisterRequest;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.aimedical.modules.patient.converter.PatientConverter;
import com.aimedical.modules.patient.dto.*;
import com.aimedical.modules.patient.entity.*;
import com.aimedical.modules.patient.repository.*;
import com.aimedical.modules.patient.service.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PatientServiceImpl implements PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientServiceImpl.class);

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PatientAllergyRepository allergyRepo;
    private final PatientChronicDiseaseRepository chronicRepo;
    private final PatientFamilyHistoryRepository familyRepo;
    private final PatientSurgeryHistoryRepository surgeryRepo;
    private final PatientMedicationHistoryRepository medicationRepo;

    public PatientServiceImpl(AuthService authService,
                               UserRepository userRepository,
                               PatientRepository patientRepository,
                               PatientAllergyRepository allergyRepo,
                               PatientChronicDiseaseRepository chronicRepo,
                               PatientFamilyHistoryRepository familyRepo,
                               PatientSurgeryHistoryRepository surgeryRepo,
                               PatientMedicationHistoryRepository medicationRepo) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.allergyRepo = allergyRepo;
        this.chronicRepo = chronicRepo;
        this.familyRepo = familyRepo;
        this.surgeryRepo = surgeryRepo;
        this.medicationRepo = medicationRepo;
    }

    // ==================== Auth ====================

    @Override
    @Transactional
    public Result<TokenResponse> register(RegisterRequest request) {
        TokenResponse token = authService.register(request);
        // Create PatientEntity profile
        User user = userRepository.findByUsername(request.getPhone())
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_NOT_FOUND));
        PatientEntity patient = new PatientEntity();
        patient.setUserId(user.getId());
        patient.setUser(user);
        patient.setRealName(request.getName());
        patient.setPhone(request.getPhone());
        patient.setGender(Gender.fromLabel(request.getGender()));
        patientRepository.save(patient);
        log.info("Patient profile created: patientId={}, userId={}", patient.getId(), user.getId());
        return Result.success(token);
    }

    @Override
    public Result<TokenResponse> login(LoginRequest request) {
        TokenResponse token = authService.login(request);
        return Result.success(token);
    }

    @Override
    public Result<TokenResponse> refresh(String refreshToken) {
        var refreshed = authService.refreshToken(refreshToken);
        TokenResponse token = new TokenResponse(refreshed.accessToken(), refreshed.refreshToken(), refreshed.expiresIn());
        return Result.success(token);
    }

    @Override
    public Result<Void> logout(String accessToken) {
        authService.logout(accessToken);
        return Result.success(null);
    }

    // ==================== Profile ====================

    @Override
    @Transactional(readOnly = true)
    public Result<PatientDto> getProfile() {
        CurrentUserResponse currentUser = authService.getCurrentUser();
        PatientEntity patient = patientRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_NOT_FOUND));
        return Result.success(PatientConverter.toDto(patient));
    }

    @Override
    @Transactional
    public Result<PatientDto> updateProfile(PatientProfileUpdateRequest request) {
        CurrentUserResponse currentUser = authService.getCurrentUser();
        PatientEntity patient = patientRepository.findByUserId(currentUser.getUserId())
                .orElseGet(() -> createPatientProfile(currentUser.getUserId()));

        if (request.getName() != null) {
            patient.setRealName(request.getName());
        }
        if (request.getGender() != null) {
            patient.setGender(Gender.fromLabel(request.getGender()));
        }
        if (request.getPhone() != null) {
            patient.setPhone(request.getPhone());
        }
        if (request.getEmail() != null || request.getAge() != null) {
            User user = userRepository.findById(currentUser.getUserId())
                    .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_NOT_FOUND));
            if (request.getEmail() != null) user.setEmail(request.getEmail());
            if (request.getAge() != null) user.setAge(request.getAge());
            userRepository.save(user);
        }
        if (request.getEmergencyContact() != null) {
            patient.setEmergencyContact(request.getEmergencyContact());
        }
        patientRepository.save(patient);
        return Result.success(PatientConverter.toDto(patient));
    }


    // ==================== Health Record ====================

    @Override
    @Transactional(readOnly = true)
    public Result<HealthRecordSummaryResponse> getHealthRecord() {
        CurrentUserResponse currentUser = authService.getCurrentUser();
        PatientEntity patient = patientRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_NOT_FOUND));
        return Result.success(PatientConverter.toHealthRecordSummary(patient));
    }

    // --- Allergy ---
    @Override
    @Transactional
    public Result<AllergyResponse> addAllergy(AllergyRequest request) {
        PatientEntity patient = getCurrentPatient();
        PatientAllergy entity = PatientConverter.toAllergyEntity(request, patient);
        entity = allergyRepo.save(entity);
        return Result.success(PatientConverter.toAllergyResponse(entity));
    }

    @Override
    @Transactional
    public Result<AllergyResponse> updateAllergy(Long allergyId, AllergyRequest request) {
        PatientEntity patient = getCurrentPatient();
        PatientAllergy entity = allergyRepo.findById(allergyId)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND));
        if (!entity.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN);
        }
        entity.setAllergen(request.getAllergen());
        entity.setReactionType(request.getReactionType());
        entity.setSeverity(request.getSeverity());
        entity.setOccurredAt(parseDate(request.getOccurredAt()));
        entity = allergyRepo.save(entity);
        return Result.success(PatientConverter.toAllergyResponse(entity));
    }

    @Override
    @Transactional
    public Result<Void> deleteAllergy(Long allergyId) {
        PatientEntity patient = getCurrentPatient();
        PatientAllergy entity = allergyRepo.findById(allergyId)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND));
        if (!entity.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN);
        }
        allergyRepo.delete(entity);
        return Result.success(null);
    }

    // --- Chronic Disease ---
    @Override
    @Transactional
    public Result<ChronicDiseaseResponse> addChronicDisease(ChronicDiseaseRequest request) {
        PatientEntity patient = getCurrentPatient();
        PatientChronicDisease entity = PatientConverter.toChronicEntity(request, patient);
        entity = chronicRepo.save(entity);
        return Result.success(PatientConverter.toChronicResponse(entity));
    }

    @Override
    @Transactional
    public Result<ChronicDiseaseResponse> updateChronicDisease(Long id, ChronicDiseaseRequest request) {
        PatientEntity patient = getCurrentPatient();
        PatientChronicDisease entity = chronicRepo.findById(id)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND));
        if (!entity.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN);
        }
        entity.setDiseaseName(request.getDiseaseName());
        entity.setDiagnosedAt(parseDate(request.getDiagnosedAt()));
        entity.setCurrentStatus(request.getCurrentStatus());
        entity = chronicRepo.save(entity);
        return Result.success(PatientConverter.toChronicResponse(entity));
    }

    @Override
    @Transactional
    public Result<Void> deleteChronicDisease(Long id) {
        PatientEntity patient = getCurrentPatient();
        PatientChronicDisease entity = chronicRepo.findById(id)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND));
        if (!entity.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN);
        }
        chronicRepo.delete(entity);
        return Result.success(null);
    }

    // --- Family History ---
    @Override
    @Transactional
    public Result<FamilyHistoryResponse> addFamilyHistory(FamilyHistoryRequest request) {
        PatientEntity patient = getCurrentPatient();
        PatientFamilyHistory entity = PatientConverter.toFamilyEntity(request, patient);
        entity = familyRepo.save(entity);
        return Result.success(PatientConverter.toFamilyResponse(entity));
    }

    @Override
    @Transactional
    public Result<FamilyHistoryResponse> updateFamilyHistory(Long id, FamilyHistoryRequest request) {
        PatientEntity patient = getCurrentPatient();
        PatientFamilyHistory entity = familyRepo.findById(id)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND));
        if (!entity.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN);
        }
        entity.setRelationship(request.getRelationship());
        entity.setDiseaseName(request.getDiseaseName());
        entity.setNote(request.getNote());
        entity = familyRepo.save(entity);
        return Result.success(PatientConverter.toFamilyResponse(entity));
    }

    @Override
    @Transactional
    public Result<Void> deleteFamilyHistory(Long id) {
        PatientEntity patient = getCurrentPatient();
        PatientFamilyHistory entity = familyRepo.findById(id)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND));
        if (!entity.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN);
        }
        familyRepo.delete(entity);
        return Result.success(null);
    }

    // --- Surgery ---
    @Override
    @Transactional
    public Result<SurgeryHistoryResponse> addSurgery(SurgeryHistoryRequest request) {
        PatientEntity patient = getCurrentPatient();
        PatientSurgeryHistory entity = PatientConverter.toSurgeryEntity(request, patient);
        entity = surgeryRepo.save(entity);
        return Result.success(PatientConverter.toSurgeryResponse(entity));
    }

    @Override
    @Transactional
    public Result<SurgeryHistoryResponse> updateSurgery(Long id, SurgeryHistoryRequest request) {
        PatientEntity patient = getCurrentPatient();
        PatientSurgeryHistory entity = surgeryRepo.findById(id)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND));
        if (!entity.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN);
        }
        entity.setSurgeryName(request.getSurgeryName());
        entity.setSurgeryAt(parseDate(request.getSurgeryAt()));
        entity.setHospital(request.getHospital());
        entity = surgeryRepo.save(entity);
        return Result.success(PatientConverter.toSurgeryResponse(entity));
    }

    @Override
    @Transactional
    public Result<Void> deleteSurgery(Long id) {
        PatientEntity patient = getCurrentPatient();
        PatientSurgeryHistory entity = surgeryRepo.findById(id)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND));
        if (!entity.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN);
        }
        surgeryRepo.delete(entity);
        return Result.success(null);
    }

    // --- Medication ---
    @Override
    @Transactional
    public Result<MedicationHistoryResponse> addMedication(MedicationHistoryRequest request) {
        PatientEntity patient = getCurrentPatient();
        PatientMedicationHistory entity = PatientConverter.toMedicationEntity(request, patient);
        entity = medicationRepo.save(entity);
        return Result.success(PatientConverter.toMedicationResponse(entity));
    }

    @Override
    @Transactional
    public Result<MedicationHistoryResponse> updateMedication(Long id, MedicationHistoryRequest request) {
        PatientEntity patient = getCurrentPatient();
        PatientMedicationHistory entity = medicationRepo.findById(id)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND));
        if (!entity.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN);
        }
        entity.setDrugName(request.getDrugName());
        entity.setReason(request.getReason());
        entity.setStartedAt(parseDate(request.getStartedAt()));
        entity.setEndedAt(parseDate(request.getEndedAt()));
        entity = medicationRepo.save(entity);
        return Result.success(PatientConverter.toMedicationResponse(entity));
    }

    @Override
    @Transactional
    public Result<Void> deleteMedication(Long id) {
        PatientEntity patient = getCurrentPatient();
        PatientMedicationHistory entity = medicationRepo.findById(id)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_NOT_FOUND));
        if (!entity.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException(PatientErrorCode.PATIENT_HEALTH_RECORD_FORBIDDEN);
        }
        medicationRepo.delete(entity);
        return Result.success(null);
    }

    // ==================== Helpers ====================

    private PatientEntity getCurrentPatient() {
        CurrentUserResponse currentUser = authService.getCurrentUser();
        return patientRepository.findByUserId(currentUser.getUserId())
                .orElseGet(() -> createPatientProfile(currentUser.getUserId()));
    }

    private PatientEntity createPatientProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_NOT_FOUND));
        PatientEntity patient = new PatientEntity();
        patient.setUserId(user.getId());
        patient.setUser(user);
        // Initialize patient profile fields from user data; fallback nickname→username
        patient.setRealName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        patient.setPhone(user.getPhone());
        patient.setGender(Gender.fromLabel(user.getGender()));
        return patientRepository.save(patient);
    }

    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
