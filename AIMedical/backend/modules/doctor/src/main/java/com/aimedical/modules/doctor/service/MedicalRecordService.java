package com.aimedical.modules.doctor.service;

import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.dto.request.MedicalRecordCreateRequest;
import com.aimedical.modules.doctor.dto.response.MedicalRecordResponse;
import com.aimedical.modules.doctor.dto.response.MedicalRecordTemplateResponse;

import java.util.List;

/**
 * 病历服务（含版本管理 DRAFT/OFFICIAL）。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface MedicalRecordService {

    Result<MedicalRecordResponse> createOrUpdateDraft(MedicalRecordCreateRequest request, Long doctorUserId);

    Result<MedicalRecordResponse> publish(Long id, Long doctorUserId);

    Result<MedicalRecordResponse> getById(Long id);

    Result<List<MedicalRecordResponse>> listByPatient(Long patientId);

    Result<List<MedicalRecordTemplateResponse>> listTemplatesByDepartment(String department);
}
