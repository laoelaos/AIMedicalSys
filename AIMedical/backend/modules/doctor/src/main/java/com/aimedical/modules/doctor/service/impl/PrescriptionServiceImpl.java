package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.converter.PrescriptionConverter;
import com.aimedical.modules.doctor.dto.request.PrescriptionAuditRequest;
import com.aimedical.modules.doctor.dto.request.PrescriptionCreateRequest;
import com.aimedical.modules.doctor.dto.response.PrescriptionResponse;
import com.aimedical.modules.doctor.entity.PrescriptionEntity;
import com.aimedical.modules.doctor.entity.PrescriptionItemEntity;
import com.aimedical.modules.doctor.entity.PrescriptionStatus;
import com.aimedical.modules.doctor.repository.DoctorRepository;
import com.aimedical.modules.doctor.repository.PrescriptionItemRepository;
import com.aimedical.modules.doctor.repository.PrescriptionRepository;
import com.aimedical.modules.doctor.service.PrescriptionService;
// T7 已知技术债务：doctor 模块直接依赖 patient 模块的 Repository，形成编译期硬依赖。
// 完整解耦应在 common-module-api 定义 PatientInfoPort 接口，由 patient 模块实现，
// doctor 模块仅依赖接口。Phase 3 范围内保留该依赖，待后续重构。
import com.aimedical.modules.patient.entity.PatientEntity;
import com.aimedical.modules.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 处方服务实现。
 *
 * <p>状态机：DRAFT -> PENDING_REVIEW -> APPROVED / REJECTED。
 * 仅 DRAFT/REJECTED 可提交审核；仅 PENDING_REVIEW 可审核。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PrescriptionConverter converter;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   PrescriptionItemRepository prescriptionItemRepository,
                                   PatientRepository patientRepository,
                                   DoctorRepository doctorRepository,
                                   PrescriptionConverter converter) {
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.converter = converter;
    }

    @Override
    @Transactional
    public Result<PrescriptionResponse> create(PrescriptionCreateRequest request, Long doctorUserId) {
        // 校验患者存在并获取姓名
        Optional<PatientEntity> patientOpt = patientRepository.findById(request.patientId());
        if (patientOpt.isEmpty()) {
            return Result.fail(GlobalErrorCode.NOT_FOUND.getCode(), "患者档案不存在");
        }
        String patientName = patientOpt.get().getRealName();

        // 查询医生科室（无档案时部门置空，不阻断开方）
        String department = doctorRepository.findByUserId(doctorUserId)
                .map(d -> d.getDepartment())
                .orElse(null);

        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setPatientId(request.patientId());
        entity.setPatientName(patientName);
        entity.setDoctorId(doctorUserId);
        entity.setDepartment(department);
        entity.setStatus(request.submitForReview()
                ? PrescriptionStatus.PENDING_REVIEW.getCode()
                : PrescriptionStatus.DRAFT.getCode());
        entity.setDiagnosis(request.diagnosis());
        entity.setRemark(request.remark());
        entity.setAiChecked(false);

        // 明细（设置双向关联，由 @ManyToOne 写入 prescription_id 外键）
        List<PrescriptionItemEntity> items = new ArrayList<>();
        for (var itemReq : request.items()) {
            PrescriptionItemEntity item = converter.toItemEntity(itemReq);
            item.setPrescription(entity);
            items.add(item);
        }
        entity.setItems(items);

        PrescriptionEntity saved = prescriptionRepository.save(entity);
        return Result.success(converter.toResponse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<PrescriptionResponse> getById(Long id) {
        return prescriptionRepository.findById(id)
                .map(converter::toResponse)
                .map(Result::success)
                .orElseGet(() -> Result.fail(GlobalErrorCode.PRESCRIPTION_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<PrescriptionResponse>> listByPatient(Long patientId) {
        List<PrescriptionResponse> list = prescriptionRepository
                .findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(converter::toResponse)
                .toList();
        return Result.success(list);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<PrescriptionResponse>> listByDoctor(Long doctorUserId) {
        List<PrescriptionResponse> list = prescriptionRepository
                .findByDoctorIdOrderByCreatedAtDesc(doctorUserId)
                .stream()
                .map(converter::toResponse)
                .toList();
        return Result.success(list);
    }

    @Override
    @Transactional
    public Result<PrescriptionResponse> submitForReview(Long id, Long doctorUserId) {
        Optional<PrescriptionEntity> opt = prescriptionRepository.findById(id);
        if (opt.isEmpty()) {
            return Result.fail(GlobalErrorCode.PRESCRIPTION_NOT_FOUND);
        }
        PrescriptionEntity entity = opt.get();
        // 校验处方归属当前医生，防止越权操作他人处方
        if (!entity.getDoctorId().equals(doctorUserId)) {
            return Result.fail(GlobalErrorCode.FORBIDDEN, "无权操作他人处方");
        }
        String current = entity.getStatus();
        // 仅 DRAFT / REJECTED 可提交审核
        if (!PrescriptionStatus.DRAFT.getCode().equals(current)
                && !PrescriptionStatus.REJECTED.getCode().equals(current)) {
            return Result.fail(GlobalErrorCode.PRESCRIPTION_INVALID_STATE);
        }
        // 校验处方明细非空：禁止无明细的处方进入审核流程
        if (entity.getItems() == null || entity.getItems().isEmpty()) {
            return Result.fail(GlobalErrorCode.PARAM_INVALID.getCode(), "处方明细不能为空，请添加至少一项药品");
        }
        entity.setStatus(PrescriptionStatus.PENDING_REVIEW.getCode());
        PrescriptionEntity saved = prescriptionRepository.save(entity);
        return Result.success(converter.toResponse(saved));
    }

    @Override
    @Transactional
    public Result<PrescriptionResponse> audit(Long id, PrescriptionAuditRequest request, Long auditorUserId) {
        // 校验审核操作人：角色权限由 Controller 层 @PreAuthorize("hasRole('DOCTOR')") 保障；
        // Phase 3 简化允许医生自审，生产环境应由药师/上级医生独立审核。
        if (auditorUserId == null) {
            return Result.fail(GlobalErrorCode.UNAUTHORIZED.getCode(), "无法获取审核操作人信息");
        }
        Optional<PrescriptionEntity> opt = prescriptionRepository.findById(id);
        if (opt.isEmpty()) {
            return Result.fail(GlobalErrorCode.PRESCRIPTION_NOT_FOUND);
        }
        PrescriptionEntity entity = opt.get();
        if (!PrescriptionStatus.PENDING_REVIEW.getCode().equals(entity.getStatus())) {
            return Result.fail(GlobalErrorCode.PRESCRIPTION_NOT_AUDITABLE);
        }
        boolean approve = Boolean.TRUE.equals(request.approve());
        // 驳回时校验驳回原因必填，避免无理由驳回
        if (!approve && (request.auditRemark() == null || request.auditRemark().isBlank())) {
            return Result.fail(GlobalErrorCode.PARAM_INVALID.getCode(), "驳回时必须填写驳回原因");
        }
        entity.setStatus(approve
                ? PrescriptionStatus.APPROVED.getCode()
                : PrescriptionStatus.REJECTED.getCode());
        entity.setAuditRemark(request.auditRemark());
        entity.setAuditedBy(auditorUserId);
        entity.setAuditedAt(LocalDateTime.now());
        PrescriptionEntity saved = prescriptionRepository.save(entity);
        return Result.success(converter.toResponse(saved));
    }
}
