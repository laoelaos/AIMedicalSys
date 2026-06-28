package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.converter.MedicalRecordConverter;
import com.aimedical.modules.doctor.dto.request.MedicalRecordCreateRequest;
import com.aimedical.modules.doctor.dto.response.MedicalRecordResponse;
import com.aimedical.modules.doctor.dto.response.MedicalRecordTemplateResponse;
import com.aimedical.modules.doctor.entity.MedicalRecordEntity;
import com.aimedical.modules.doctor.entity.MedicalRecordStatus;
import com.aimedical.modules.doctor.entity.MedicalRecordTemplateEntity;
import com.aimedical.modules.doctor.repository.DoctorRepository;
import com.aimedical.modules.doctor.repository.MedicalRecordRepository;
import com.aimedical.modules.doctor.repository.MedicalRecordTemplateRepository;
import com.aimedical.modules.doctor.service.MedicalRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 病历服务实现（含版本管理）。
 *
 * <p>版本规则：
 * <ul>
 *   <li>同一患者+医生仅保留一份 DRAFT 草稿，重复保存即更新该草稿</li>
 *   <li>草稿 versionNo=0；发布后 versionNo = (该患者最新正式版本号)+1，状态转为 OFFICIAL</li>
 * </ul>
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private static final int DRAFT_VERSION_NO = 0;

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordTemplateRepository templateRepository;
    private final DoctorRepository doctorRepository;
    private final MedicalRecordConverter converter;

    public MedicalRecordServiceImpl(MedicalRecordRepository medicalRecordRepository,
                                    MedicalRecordTemplateRepository templateRepository,
                                    DoctorRepository doctorRepository,
                                    MedicalRecordConverter converter) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.templateRepository = templateRepository;
        this.doctorRepository = doctorRepository;
        this.converter = converter;
    }

    @Override
    @Transactional
    public Result<MedicalRecordResponse> createOrUpdateDraft(MedicalRecordCreateRequest request, Long doctorUserId) {
        // 查找该患者+医生的现有草稿
        List<MedicalRecordEntity> drafts = medicalRecordRepository
                .findByPatientIdAndStatusOrderByVersionNoDesc(request.patientId(), MedicalRecordStatus.DRAFT.getCode())
                .stream()
                .filter(r -> doctorUserId.equals(r.getDoctorId()))
                .toList();

        String department = doctorRepository.findByUserId(doctorUserId)
                .map(d -> d.getDepartment())
                .orElse(null);

        MedicalRecordEntity entity;
        if (drafts.isEmpty()) {
            entity = new MedicalRecordEntity();
            entity.setPatientId(request.patientId());
            entity.setDoctorId(doctorUserId);
            entity.setDepartment(department);
            entity.setVersionNo(DRAFT_VERSION_NO);
            entity.setStatus(MedicalRecordStatus.DRAFT.getCode());
            entity.setAiGenerated(false);
        } else {
            entity = drafts.get(0);
        }

        entity.setTemplateId(request.templateId());
        entity.setPrescriptionId(request.prescriptionId());
        entity.setChiefComplaint(request.chiefComplaint());
        entity.setPresentIllness(request.presentIllness());
        entity.setPastHistory(request.pastHistory());
        entity.setDiagnosis(request.diagnosis());
        entity.setTreatmentPlan(request.treatmentPlan());
        entity.setRemark(request.remark());

        MedicalRecordEntity saved = medicalRecordRepository.save(entity);

        // 发布为正式版本
        if (request.publish()) {
            return publish(saved.getId(), doctorUserId);
        }
        return Result.success(converter.toResponse(saved));
    }

    @Override
    @Transactional
    public Result<MedicalRecordResponse> publish(Long id, Long doctorUserId) {
        Optional<MedicalRecordEntity> opt = medicalRecordRepository.findById(id);
        if (opt.isEmpty()) {
            return Result.fail(GlobalErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }
        MedicalRecordEntity entity = opt.get();
        // 校验病历归属当前医生，防止越权操作他人病历
        if (!entity.getDoctorId().equals(doctorUserId)) {
            return Result.fail(GlobalErrorCode.FORBIDDEN, "无权操作他人病历");
        }
        if (!MedicalRecordStatus.DRAFT.getCode().equals(entity.getStatus())) {
            return Result.fail(GlobalErrorCode.MEDICAL_RECORD_INVALID_STATE);
        }
        // 计算新版本号：该患者最新正式版本号 + 1
        int nextVersion = medicalRecordRepository
                .findByPatientIdAndStatusOrderByVersionNoDesc(entity.getPatientId(),
                        MedicalRecordStatus.OFFICIAL.getCode())
                .stream()
                .findFirst()
                .map(MedicalRecordEntity::getVersionNo)
                .map(v -> v + 1)
                .orElse(1);
        entity.setVersionNo(nextVersion);
        entity.setStatus(MedicalRecordStatus.OFFICIAL.getCode());
        MedicalRecordEntity saved = medicalRecordRepository.save(entity);
        return Result.success(converter.toResponse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<MedicalRecordResponse> getById(Long id) {
        return medicalRecordRepository.findById(id)
                .map(converter::toResponse)
                .map(Result::success)
                .orElseGet(() -> Result.fail(GlobalErrorCode.MEDICAL_RECORD_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<MedicalRecordResponse>> listByPatient(Long patientId) {
        List<MedicalRecordResponse> list = medicalRecordRepository
                .findByPatientIdOrderByVersionNoDesc(patientId)
                .stream()
                .map(converter::toResponse)
                .toList();
        return Result.success(list);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<MedicalRecordTemplateResponse>> listTemplatesByDepartment(String department) {
        // department 为空时返回所有启用模板，否则按科室精确匹配
        List<MedicalRecordTemplateEntity> templates = (department == null || department.isBlank())
                ? templateRepository.findByEnabled(true)
                : templateRepository.findByDepartmentAndEnabled(department, true);
        List<MedicalRecordTemplateResponse> list = templates
                .stream()
                .map(converter::toTemplateResponse)
                .toList();
        return Result.success(list);
    }
}
