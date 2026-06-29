package com.aimedical.modules.patient.service;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.dto.LoginRequest;
import com.aimedical.modules.commonmodule.api.dto.RegisterRequest;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;
import com.aimedical.modules.patient.dto.*;

public interface PatientService {

    Result<TokenResponse> register(RegisterRequest request);

    Result<TokenResponse> login(LoginRequest request);

    Result<TokenResponse> refresh(String refreshToken);

    Result<Void> logout(String accessToken);

    Result<PatientDto> getProfile();

    Result<PatientDto> updateProfile(PatientProfileUpdateRequest request);

    Result<HealthRecordSummaryResponse> getHealthRecord();

    // Allergy CRUD
    Result<AllergyResponse> addAllergy(AllergyRequest request);
    Result<AllergyResponse> updateAllergy(Long allergyId, AllergyRequest request);
    Result<Void> deleteAllergy(Long allergyId);

    // Chronic Disease CRUD
    Result<ChronicDiseaseResponse> addChronicDisease(ChronicDiseaseRequest request);
    Result<ChronicDiseaseResponse> updateChronicDisease(Long id, ChronicDiseaseRequest request);
    Result<Void> deleteChronicDisease(Long id);

    // Family History CRUD
    Result<FamilyHistoryResponse> addFamilyHistory(FamilyHistoryRequest request);
    Result<FamilyHistoryResponse> updateFamilyHistory(Long id, FamilyHistoryRequest request);
    Result<Void> deleteFamilyHistory(Long id);

    // Surgery CRUD
    Result<SurgeryHistoryResponse> addSurgery(SurgeryHistoryRequest request);
    Result<SurgeryHistoryResponse> updateSurgery(Long id, SurgeryHistoryRequest request);
    Result<Void> deleteSurgery(Long id);

    // Medication CRUD
    Result<MedicationHistoryResponse> addMedication(MedicationHistoryRequest request);
    Result<MedicationHistoryResponse> updateMedication(Long id, MedicationHistoryRequest request);
    Result<Void> deleteMedication(Long id);
}
