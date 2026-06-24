package com.aimedical.modules.patient.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.dto.LoginRequest;
import com.aimedical.modules.commonmodule.api.dto.RegisterRequest;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;
import com.aimedical.modules.patient.dto.*;
import com.aimedical.modules.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient")
@Tag(name = "患者端", description = "患者注册、登录、个人中心、健康档案")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // ==================== Auth ====================

    @PostMapping("/register")
    @Operation(summary = "患者注册")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return patientService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "患者登录")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return patientService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌")
    public Result<TokenResponse> refresh(@RequestHeader("Authorization") String authorization) {
        String token = extractBearerToken(authorization);
        return patientService.refresh(token);
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = extractBearerToken(authorization);
        return patientService.logout(token);
    }

    // ==================== Profile ====================

    @GetMapping("/profile")
    @Operation(summary = "获取个人中心信息")
    public Result<PatientDto> getProfile() {
        return patientService.getProfile();
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人信息")
    public Result<PatientDto> updateProfile(@Valid @RequestBody PatientProfileUpdateRequest request) {
        return patientService.updateProfile(request);
    }

    // ==================== Health Record ====================

    @GetMapping("/health-record")
    @Operation(summary = "获取健康档案全量")
    public Result<HealthRecordSummaryResponse> getHealthRecord() {
        return patientService.getHealthRecord();
    }

    // --- Allergy ---
    @PostMapping("/health-record/allergies")
    @Operation(summary = "添加过敏史")
    public Result<AllergyResponse> addAllergy(@Valid @RequestBody AllergyRequest request) {
        return patientService.addAllergy(request);
    }

    @PutMapping("/health-record/allergies/{id}")
    @Operation(summary = "更新过敏史")
    public Result<AllergyResponse> updateAllergy(@PathVariable Long id, @Valid @RequestBody AllergyRequest request) {
        return patientService.updateAllergy(id, request);
    }

    @DeleteMapping("/health-record/allergies/{id}")
    @Operation(summary = "删除过敏史")
    public Result<Void> deleteAllergy(@PathVariable Long id) {
        return patientService.deleteAllergy(id);
    }

    // --- Chronic Disease ---
    @PostMapping("/health-record/chronic-diseases")
    @Operation(summary = "添加慢病史")
    public Result<ChronicDiseaseResponse> addChronicDisease(@Valid @RequestBody ChronicDiseaseRequest request) {
        return patientService.addChronicDisease(request);
    }

    @PutMapping("/health-record/chronic-diseases/{id}")
    @Operation(summary = "更新慢病史")
    public Result<ChronicDiseaseResponse> updateChronicDisease(@PathVariable Long id, @Valid @RequestBody ChronicDiseaseRequest request) {
        return patientService.updateChronicDisease(id, request);
    }

    @DeleteMapping("/health-record/chronic-diseases/{id}")
    @Operation(summary = "删除慢病史")
    public Result<Void> deleteChronicDisease(@PathVariable Long id) {
        return patientService.deleteChronicDisease(id);
    }

    // --- Family History ---
    @PostMapping("/health-record/family-history")
    @Operation(summary = "添加家族史")
    public Result<FamilyHistoryResponse> addFamilyHistory(@Valid @RequestBody FamilyHistoryRequest request) {
        return patientService.addFamilyHistory(request);
    }

    @PutMapping("/health-record/family-history/{id}")
    @Operation(summary = "更新家族史")
    public Result<FamilyHistoryResponse> updateFamilyHistory(@PathVariable Long id, @Valid @RequestBody FamilyHistoryRequest request) {
        return patientService.updateFamilyHistory(id, request);
    }

    @DeleteMapping("/health-record/family-history/{id}")
    @Operation(summary = "删除家族史")
    public Result<Void> deleteFamilyHistory(@PathVariable Long id) {
        return patientService.deleteFamilyHistory(id);
    }

    // --- Surgery ---
    @PostMapping("/health-record/surgeries")
    @Operation(summary = "添加手术史")
    public Result<SurgeryHistoryResponse> addSurgery(@Valid @RequestBody SurgeryHistoryRequest request) {
        return patientService.addSurgery(request);
    }

    @PutMapping("/health-record/surgeries/{id}")
    @Operation(summary = "更新手术史")
    public Result<SurgeryHistoryResponse> updateSurgery(@PathVariable Long id, @Valid @RequestBody SurgeryHistoryRequest request) {
        return patientService.updateSurgery(id, request);
    }

    @DeleteMapping("/health-record/surgeries/{id}")
    @Operation(summary = "删除手术史")
    public Result<Void> deleteSurgery(@PathVariable Long id) {
        return patientService.deleteSurgery(id);
    }

    // --- Medication ---
    @PostMapping("/health-record/medications")
    @Operation(summary = "添加用药史")
    public Result<MedicationHistoryResponse> addMedication(@Valid @RequestBody MedicationHistoryRequest request) {
        return patientService.addMedication(request);
    }

    @PutMapping("/health-record/medications/{id}")
    @Operation(summary = "更新用药史")
    public Result<MedicationHistoryResponse> updateMedication(@PathVariable Long id, @Valid @RequestBody MedicationHistoryRequest request) {
        return patientService.updateMedication(id, request);
    }

    @DeleteMapping("/health-record/medications/{id}")
    @Operation(summary = "删除用药史")
    public Result<Void> deleteMedication(@PathVariable Long id) {
        return patientService.deleteMedication(id);
    }

    // ==================== Helper ====================

    private String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return "";
    }
}
