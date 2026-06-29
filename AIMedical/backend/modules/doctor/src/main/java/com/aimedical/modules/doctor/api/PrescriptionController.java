package com.aimedical.modules.doctor.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.CurrentUser;
import com.aimedical.modules.doctor.dto.request.PrescriptionAuditRequest;
import com.aimedical.modules.doctor.dto.request.PrescriptionCreateRequest;
import com.aimedical.modules.doctor.dto.response.PrescriptionResponse;
import com.aimedical.modules.doctor.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 处方中心控制器。
 *
 * <p>提供处方的创建、查询、提交审核、审核（通过/驳回）接口。
 * 全部需要 DOCTOR 角色。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/doctor/prescriptions")
@PreAuthorize("hasRole('DOCTOR')")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final CurrentUser currentUser;

    public PrescriptionController(PrescriptionService prescriptionService, CurrentUser currentUser) {
        this.prescriptionService = prescriptionService;
        this.currentUser = currentUser;
    }

    /**
     * 创建处方。
     *
     * <p>创建后默认为 DRAFT 草稿状态；若请求体 {@code submitForReview=true} 则直接进入待审状态。
     */
    @PostMapping
    public Result<PrescriptionResponse> create(@Valid @RequestBody PrescriptionCreateRequest request) {
        return prescriptionService.create(request, currentDoctorId());
    }

    /**
     * 获取处方详情。
     */
    @GetMapping("/{id}")
    public Result<PrescriptionResponse> get(@PathVariable Long id) {
        return prescriptionService.getById(id, currentDoctorId());
    }

    /**
     * 按患者查询处方列表（仅当前医生为该患者开具的处方）。
     *
     * @param patientId 患者档案ID
     */
    @GetMapping
    public Result<List<PrescriptionResponse>> listByPatient(@RequestParam Long patientId) {
        return prescriptionService.listByPatient(patientId, currentDoctorId());
    }

    /**
     * 提交处方审核（DRAFT/REJECTED -> PENDING_REVIEW）。
     */
    @PostMapping("/{id}/submit")
    public Result<PrescriptionResponse> submitForReview(@PathVariable Long id) {
        return prescriptionService.submitForReview(id, currentDoctorId());
    }

    /**
     * 审核处方（PENDING_REVIEW -> APPROVED/REJECTED）。
     *
     * <p>Phase 3 简化：暂由医生角色审核；生产环境应由药师/上级医生独立审核。
     */
    @PostMapping("/{id}/audit")
    public Result<PrescriptionResponse> audit(@PathVariable Long id,
                                              @Valid @RequestBody PrescriptionAuditRequest request) {
        return prescriptionService.audit(id, request, currentDoctorId());
    }

    private Long currentDoctorId() {
        Long userId = currentUser.getUserId();
        if (userId == null) {
            throw new IllegalStateException("无法获取当前登录医生ID");
        }
        return userId;
    }
}
