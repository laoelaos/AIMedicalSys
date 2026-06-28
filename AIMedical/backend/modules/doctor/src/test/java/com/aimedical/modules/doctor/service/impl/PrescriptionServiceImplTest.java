package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.converter.PrescriptionConverter;
import com.aimedical.modules.doctor.dto.request.PrescriptionAuditRequest;
import com.aimedical.modules.doctor.dto.request.PrescriptionCreateRequest;
import com.aimedical.modules.doctor.dto.request.PrescriptionItemRequest;
import com.aimedical.modules.doctor.dto.response.PrescriptionResponse;
import com.aimedical.modules.doctor.entity.PrescriptionEntity;
import com.aimedical.modules.doctor.entity.PrescriptionItemEntity;
import com.aimedical.modules.doctor.entity.PrescriptionStatus;
import com.aimedical.modules.doctor.repository.DoctorRepository;
import com.aimedical.modules.doctor.repository.PrescriptionItemRepository;
import com.aimedical.modules.doctor.repository.PrescriptionRepository;
import com.aimedical.modules.doctor.service.PrescriptionService;
import com.aimedical.modules.patient.entity.PatientEntity;
import com.aimedical.modules.patient.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PrescriptionServiceImpl} 单元测试。
 *
 * <p>覆盖状态机：DRAFT -> PENDING_REVIEW -> APPROVED / REJECTED；
 * 覆盖越权校验（仅处方归属医生可提交审核）。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PrescriptionItemRepository prescriptionItemRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PrescriptionConverter converter;

    @InjectMocks
    private PrescriptionServiceImpl service;

    private static final Long DOCTOR_USER_ID = 200L;
    private static final Long OTHER_DOCTOR_ID = 999L;
    private static final Long PATIENT_ID = 100L;
    private static final Long PRESCRIPTION_ID = 1L;

    // ---------- create ----------

    @Test
    void create_shouldReturnNotFoundWhenPatientNotExists() {
        PrescriptionCreateRequest request = buildCreateRequest(false);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());

        Result<PrescriptionResponse> result = service.create(request, DOCTOR_USER_ID);

        assertEquals(GlobalErrorCode.NOT_FOUND.getCode(), result.getCode());
    }

    @Test
    void create_shouldSaveDraftWhenSubmitForReviewFalse() {
        PrescriptionCreateRequest request = buildCreateRequest(false);
        PatientEntity patient = buildPatient();
        PrescriptionItemEntity itemEntity = new PrescriptionItemEntity();
        PrescriptionEntity saved = new PrescriptionEntity();
        saved.setId(PRESCRIPTION_ID);
        saved.setStatus(PrescriptionStatus.DRAFT.getCode());
        PrescriptionResponse response = buildResponse(PRESCRIPTION_ID, PrescriptionStatus.DRAFT.getCode());

        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByUserId(DOCTOR_USER_ID)).thenReturn(Optional.empty());
        when(converter.toItemEntity(any(PrescriptionItemRequest.class))).thenReturn(itemEntity);
        when(prescriptionRepository.save(any(PrescriptionEntity.class))).thenReturn(saved);
        when(converter.toResponse(saved)).thenReturn(response);

        Result<PrescriptionResponse> result = service.create(request, DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(PrescriptionStatus.DRAFT.getCode(), result.getData().status());
        verify(prescriptionRepository).save(any(PrescriptionEntity.class));
    }

    @Test
    void create_shouldSavePendingReviewWhenSubmitForReviewTrue() {
        PrescriptionCreateRequest request = buildCreateRequest(true);
        PatientEntity patient = buildPatient();
        PrescriptionItemEntity itemEntity = new PrescriptionItemEntity();
        PrescriptionEntity saved = new PrescriptionEntity();
        saved.setId(PRESCRIPTION_ID);
        saved.setStatus(PrescriptionStatus.PENDING_REVIEW.getCode());
        PrescriptionResponse response = buildResponse(PRESCRIPTION_ID, PrescriptionStatus.PENDING_REVIEW.getCode());

        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByUserId(DOCTOR_USER_ID)).thenReturn(Optional.empty());
        when(converter.toItemEntity(any(PrescriptionItemRequest.class))).thenReturn(itemEntity);
        when(prescriptionRepository.save(any(PrescriptionEntity.class))).thenReturn(saved);
        when(converter.toResponse(saved)).thenReturn(response);

        Result<PrescriptionResponse> result = service.create(request, DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(PrescriptionStatus.PENDING_REVIEW.getCode(), result.getData().status());
    }

    // ---------- getById ----------

    @Test
    void getById_shouldReturnNotFoundWhenNotExists() {
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.empty());

        Result<PrescriptionResponse> result = service.getById(PRESCRIPTION_ID);

        assertEquals(GlobalErrorCode.PRESCRIPTION_NOT_FOUND.getCode(), result.getCode());
    }

    @Test
    void getById_shouldReturnResponseWhenExists() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(PRESCRIPTION_ID);
        PrescriptionResponse response = buildResponse(PRESCRIPTION_ID, PrescriptionStatus.DRAFT.getCode());
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(entity));
        when(converter.toResponse(entity)).thenReturn(response);

        Result<PrescriptionResponse> result = service.getById(PRESCRIPTION_ID);

        assertEquals("SUCCESS", result.getCode());
        assertNotNull(result.getData());
    }

    // ---------- listByPatient ----------

    @Test
    void listByPatient_shouldReturnList() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(PRESCRIPTION_ID);
        PrescriptionResponse response = buildResponse(PRESCRIPTION_ID, PrescriptionStatus.DRAFT.getCode());
        when(prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(PATIENT_ID))
                .thenReturn(List.of(entity));
        when(converter.toResponse(entity)).thenReturn(response);

        Result<List<PrescriptionResponse>> result = service.listByPatient(PATIENT_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(1, result.getData().size());
    }

    // ---------- listByDoctor ----------

    @Test
    void listByDoctor_shouldReturnList() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(PRESCRIPTION_ID);
        PrescriptionResponse response = buildResponse(PRESCRIPTION_ID, PrescriptionStatus.DRAFT.getCode());
        when(prescriptionRepository.findByDoctorIdOrderByCreatedAtDesc(DOCTOR_USER_ID))
                .thenReturn(List.of(entity));
        when(converter.toResponse(entity)).thenReturn(response);

        Result<List<PrescriptionResponse>> result = service.listByDoctor(DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(1, result.getData().size());
    }

    // ---------- submitForReview ----------

    @Test
    void submitForReview_shouldReturnNotFoundWhenNotExists() {
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.empty());

        Result<PrescriptionResponse> result = service.submitForReview(PRESCRIPTION_ID, DOCTOR_USER_ID);

        assertEquals(GlobalErrorCode.PRESCRIPTION_NOT_FOUND.getCode(), result.getCode());
    }

    @Test
    void submitForReview_shouldReturnForbiddenWhenNotOwner() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(PRESCRIPTION_ID);
        entity.setDoctorId(OTHER_DOCTOR_ID);
        entity.setStatus(PrescriptionStatus.DRAFT.getCode());
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(entity));

        Result<PrescriptionResponse> result = service.submitForReview(PRESCRIPTION_ID, DOCTOR_USER_ID);

        assertEquals(GlobalErrorCode.FORBIDDEN.getCode(), result.getCode());
    }

    @Test
    void submitForReview_shouldReturnInvalidStateWhenStatusIsApproved() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(PRESCRIPTION_ID);
        entity.setDoctorId(DOCTOR_USER_ID);
        entity.setStatus(PrescriptionStatus.APPROVED.getCode());
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(entity));

        Result<PrescriptionResponse> result = service.submitForReview(PRESCRIPTION_ID, DOCTOR_USER_ID);

        assertEquals(GlobalErrorCode.PRESCRIPTION_INVALID_STATE.getCode(), result.getCode());
    }

    @Test
    void submitForReview_shouldSetPendingReviewWhenStatusIsDraft() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(PRESCRIPTION_ID);
        entity.setDoctorId(DOCTOR_USER_ID);
        entity.setStatus(PrescriptionStatus.DRAFT.getCode());
        PrescriptionResponse response = buildResponse(PRESCRIPTION_ID, PrescriptionStatus.PENDING_REVIEW.getCode());
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(entity));
        when(prescriptionRepository.save(entity)).thenReturn(entity);
        when(converter.toResponse(entity)).thenReturn(response);

        Result<PrescriptionResponse> result = service.submitForReview(PRESCRIPTION_ID, DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(PrescriptionStatus.PENDING_REVIEW.getCode(), entity.getStatus());
    }

    @Test
    void submitForReview_shouldSetPendingReviewWhenStatusIsRejected() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(PRESCRIPTION_ID);
        entity.setDoctorId(DOCTOR_USER_ID);
        entity.setStatus(PrescriptionStatus.REJECTED.getCode());
        PrescriptionResponse response = buildResponse(PRESCRIPTION_ID, PrescriptionStatus.PENDING_REVIEW.getCode());
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(entity));
        when(prescriptionRepository.save(entity)).thenReturn(entity);
        when(converter.toResponse(entity)).thenReturn(response);

        Result<PrescriptionResponse> result = service.submitForReview(PRESCRIPTION_ID, DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(PrescriptionStatus.PENDING_REVIEW.getCode(), entity.getStatus());
    }

    // ---------- audit ----------

    @Test
    void audit_shouldReturnNotFoundWhenNotExists() {
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.empty());

        Result<PrescriptionResponse> result = service.audit(PRESCRIPTION_ID,
                new PrescriptionAuditRequest(true, "通过"), DOCTOR_USER_ID);

        assertEquals(GlobalErrorCode.PRESCRIPTION_NOT_FOUND.getCode(), result.getCode());
    }

    @Test
    void audit_shouldReturnNotAuditableWhenStatusIsDraft() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(PRESCRIPTION_ID);
        entity.setStatus(PrescriptionStatus.DRAFT.getCode());
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(entity));

        Result<PrescriptionResponse> result = service.audit(PRESCRIPTION_ID,
                new PrescriptionAuditRequest(true, "通过"), DOCTOR_USER_ID);

        assertEquals(GlobalErrorCode.PRESCRIPTION_NOT_AUDITABLE.getCode(), result.getCode());
    }

    @Test
    void audit_shouldSetApprovedWhenApproveTrue() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(PRESCRIPTION_ID);
        entity.setStatus(PrescriptionStatus.PENDING_REVIEW.getCode());
        PrescriptionResponse response = buildResponse(PRESCRIPTION_ID, PrescriptionStatus.APPROVED.getCode());
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(entity));
        when(prescriptionRepository.save(entity)).thenReturn(entity);
        when(converter.toResponse(entity)).thenReturn(response);

        Result<PrescriptionResponse> result = service.audit(PRESCRIPTION_ID,
                new PrescriptionAuditRequest(true, "审核通过"), DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(PrescriptionStatus.APPROVED.getCode(), entity.getStatus());
        assertEquals("审核通过", entity.getAuditRemark());
        assertEquals(DOCTOR_USER_ID, entity.getAuditedBy());
        assertNotNull(entity.getAuditedAt());
    }

    @Test
    void audit_shouldSetRejectedWhenApproveFalse() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(PRESCRIPTION_ID);
        entity.setStatus(PrescriptionStatus.PENDING_REVIEW.getCode());
        PrescriptionResponse response = buildResponse(PRESCRIPTION_ID, PrescriptionStatus.REJECTED.getCode());
        when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(entity));
        when(prescriptionRepository.save(entity)).thenReturn(entity);
        when(converter.toResponse(entity)).thenReturn(response);

        Result<PrescriptionResponse> result = service.audit(PRESCRIPTION_ID,
                new PrescriptionAuditRequest(false, "用法用量不当"), DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(PrescriptionStatus.REJECTED.getCode(), entity.getStatus());
        assertEquals("用法用量不当", entity.getAuditRemark());
    }

    // ---------- 测试数据构造 ----------

    private PrescriptionCreateRequest buildCreateRequest(boolean submitForReview) {
        return new PrescriptionCreateRequest(
                PATIENT_ID,
                "上呼吸道感染",
                "饭后服用",
                submitForReview,
                List.of(new PrescriptionItemRequest(
                        "阿莫西林", "0.25g/粒", "1粒", "口服", "每日3次",
                        new BigDecimal("6"), "粒", "无"
                ))
        );
    }

    private PatientEntity buildPatient() {
        PatientEntity patient = new PatientEntity();
        patient.setId(PATIENT_ID);
        patient.setRealName("张三");
        return patient;
    }

    private PrescriptionResponse buildResponse(Long id, String status) {
        return new PrescriptionResponse(
                id, PATIENT_ID, "张三", DOCTOR_USER_ID, "内科",
                status, "上呼吸道感染", false, null,
                null, null, "饭后服用",
                LocalDateTime.now(), LocalDateTime.now(), List.of()
        );
    }
}
