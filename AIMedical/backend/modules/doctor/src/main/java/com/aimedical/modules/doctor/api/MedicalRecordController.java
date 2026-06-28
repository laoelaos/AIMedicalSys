package com.aimedical.modules.doctor.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.CurrentUser;
import com.aimedical.modules.doctor.dto.request.MedicalRecordCreateRequest;
import com.aimedical.modules.doctor.dto.response.MedicalRecordResponse;
import com.aimedical.modules.doctor.dto.response.MedicalRecordTemplateResponse;
import com.aimedical.modules.doctor.service.MedicalRecordService;
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
 * 病历中心控制器。
 *
 * <p>提供病历的创建/更新草稿、发布为正式版本、查询详情与列表、按科室查询模板接口。
 * 全部需要 DOCTOR 角色。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/doctor/medical-records")
@PreAuthorize("hasRole('DOCTOR')")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final CurrentUser currentUser;

    public MedicalRecordController(MedicalRecordService medicalRecordService, CurrentUser currentUser) {
        this.medicalRecordService = medicalRecordService;
        this.currentUser = currentUser;
    }

    /**
     * 创建或更新草稿病历。
     *
     * <p>若 {@code publish=true}，则在保存草稿后立即发布为正式版本（DRAFT -> OFFICIAL，versionNo 自增）。
     */
    @PostMapping
    public Result<MedicalRecordResponse> create(@Valid @RequestBody MedicalRecordCreateRequest request) {
        return medicalRecordService.createOrUpdateDraft(request, currentDoctorId());
    }

    /**
     * 获取病历详情。
     */
    @GetMapping("/{id}")
    public Result<MedicalRecordResponse> get(@PathVariable Long id) {
        return medicalRecordService.getById(id);
    }

    /**
     * 按患者查询病历列表（按版本号倒序）。
     *
     * @param patientId 患者档案ID
     */
    @GetMapping
    public Result<List<MedicalRecordResponse>> listByPatient(@RequestParam Long patientId) {
        return medicalRecordService.listByPatient(patientId);
    }

    /**
     * 将草稿病历发布为正式版本。
     */
    @PostMapping("/{id}/publish")
    public Result<MedicalRecordResponse> publish(@PathVariable Long id) {
        return medicalRecordService.publish(id, currentDoctorId());
    }

    /**
     * 按科室查询启用的病历模板列表。
     *
     * @param department 科室名称
     */
    @GetMapping("/templates")
    public Result<List<MedicalRecordTemplateResponse>> listTemplates(@RequestParam(required = false) String department) {
        return medicalRecordService.listTemplatesByDepartment(department);
    }

    private Long currentDoctorId() {
        Long userId = currentUser.getUserId();
        if (userId == null) {
            throw new IllegalStateException("无法获取当前登录医生ID");
        }
        return userId;
    }
}
