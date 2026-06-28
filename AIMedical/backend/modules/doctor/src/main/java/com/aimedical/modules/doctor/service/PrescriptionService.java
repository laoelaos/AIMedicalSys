package com.aimedical.modules.doctor.service;

import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.dto.request.PrescriptionAuditRequest;
import com.aimedical.modules.doctor.dto.request.PrescriptionCreateRequest;
import com.aimedical.modules.doctor.dto.response.PrescriptionResponse;

import java.util.List;

/**
 * 处方服务。
 *
 * <p>处方状态机：DRAFT -> PENDING_REVIEW -> APPROVED / REJECTED。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface PrescriptionService {

    Result<PrescriptionResponse> create(PrescriptionCreateRequest request, Long doctorUserId);

    Result<PrescriptionResponse> getById(Long id);

    Result<List<PrescriptionResponse>> listByPatient(Long patientId);

    Result<List<PrescriptionResponse>> listByDoctor(Long doctorUserId);

    Result<PrescriptionResponse> submitForReview(Long id, Long doctorUserId);

    Result<PrescriptionResponse> audit(Long id, PrescriptionAuditRequest request, Long auditorUserId);
}
