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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MedicalRecordServiceImpl} 单元测试。
 *
 * <p>覆盖版本管理：DRAFT 草稿 -> OFFICIAL 正式（versionNo 自增）；
 * 覆盖越权校验（仅病历归属医生可发布）。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceImplTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private MedicalRecordTemplateRepository templateRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private MedicalRecordConverter converter;

    @InjectMocks
    private MedicalRecordServiceImpl service;

    private static final Long DOCTOR_USER_ID = 200L;
    private static final Long OTHER_DOCTOR_ID = 999L;
    private static final Long PATIENT_ID = 100L;
    private static final Long RECORD_ID = 1L;

    // ---------- createOrUpdateDraft ----------

    @Test
    void createOrUpdateDraft_shouldCreateNewDraftWhenNoExistingDraft() {
        MedicalRecordCreateRequest request = buildCreateRequest(false);
        MedicalRecordEntity saved = new MedicalRecordEntity();
        saved.setId(RECORD_ID);
        saved.setPatientId(PATIENT_ID);
        saved.setDoctorId(DOCTOR_USER_ID);
        saved.setStatus(MedicalRecordStatus.DRAFT.getCode());
        saved.setVersionNo(0);
        MedicalRecordResponse response = buildResponse(RECORD_ID, MedicalRecordStatus.DRAFT.getCode(), 0);

        when(medicalRecordRepository.findByPatientIdAndDoctorIdAndStatusOrderByVersionNoDesc(
                PATIENT_ID, DOCTOR_USER_ID, MedicalRecordStatus.DRAFT.getCode())).thenReturn(List.of());
        when(doctorRepository.findByUserId(DOCTOR_USER_ID)).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(any(MedicalRecordEntity.class))).thenReturn(saved);
        when(converter.toResponse(saved)).thenReturn(response);

        Result<MedicalRecordResponse> result = service.createOrUpdateDraft(request, DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(MedicalRecordStatus.DRAFT.getCode(), result.getData().status());
        verify(medicalRecordRepository).save(any(MedicalRecordEntity.class));
    }

    @Test
    void createOrUpdateDraft_shouldUpdateExistingDraftWhenFound() {
        MedicalRecordCreateRequest request = buildCreateRequest(false);
        MedicalRecordEntity existing = new MedicalRecordEntity();
        existing.setId(RECORD_ID);
        existing.setPatientId(PATIENT_ID);
        existing.setDoctorId(DOCTOR_USER_ID);
        existing.setStatus(MedicalRecordStatus.DRAFT.getCode());
        existing.setVersionNo(0);
        MedicalRecordResponse response = buildResponse(RECORD_ID, MedicalRecordStatus.DRAFT.getCode(), 0);

        when(medicalRecordRepository.findByPatientIdAndDoctorIdAndStatusOrderByVersionNoDesc(
                PATIENT_ID, DOCTOR_USER_ID, MedicalRecordStatus.DRAFT.getCode())).thenReturn(List.of(existing));
        when(doctorRepository.findByUserId(DOCTOR_USER_ID)).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(existing)).thenReturn(existing);
        when(converter.toResponse(existing)).thenReturn(response);

        Result<MedicalRecordResponse> result = service.createOrUpdateDraft(request, DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals("头痛", existing.getChiefComplaint());
        assertEquals("感冒", existing.getDiagnosis());
    }

    // ---------- publish ----------

    @Test
    void publish_shouldReturnNotFoundWhenNotExists() {
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

        Result<MedicalRecordResponse> result = service.publish(RECORD_ID, DOCTOR_USER_ID);

        assertEquals(GlobalErrorCode.MEDICAL_RECORD_NOT_FOUND.getCode(), result.getCode());
    }

    @Test
    void publish_shouldReturnForbiddenWhenNotOwner() {
        MedicalRecordEntity entity = new MedicalRecordEntity();
        entity.setId(RECORD_ID);
        entity.setDoctorId(OTHER_DOCTOR_ID);
        entity.setStatus(MedicalRecordStatus.DRAFT.getCode());
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(entity));

        Result<MedicalRecordResponse> result = service.publish(RECORD_ID, DOCTOR_USER_ID);

        assertEquals(GlobalErrorCode.FORBIDDEN.getCode(), result.getCode());
    }

    @Test
    void publish_shouldReturnInvalidStateWhenStatusIsOfficial() {
        MedicalRecordEntity entity = new MedicalRecordEntity();
        entity.setId(RECORD_ID);
        entity.setDoctorId(DOCTOR_USER_ID);
        entity.setStatus(MedicalRecordStatus.OFFICIAL.getCode());
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(entity));

        Result<MedicalRecordResponse> result = service.publish(RECORD_ID, DOCTOR_USER_ID);

        assertEquals(GlobalErrorCode.MEDICAL_RECORD_INVALID_STATE.getCode(), result.getCode());
    }

    @Test
    void publish_shouldSetOfficialAndIncrementVersionWhenNoExistingOfficial() {
        MedicalRecordEntity entity = new MedicalRecordEntity();
        entity.setId(RECORD_ID);
        entity.setPatientId(PATIENT_ID);
        entity.setDoctorId(DOCTOR_USER_ID);
        entity.setStatus(MedicalRecordStatus.DRAFT.getCode());
        entity.setVersionNo(0);
        MedicalRecordResponse response = buildResponse(RECORD_ID, MedicalRecordStatus.OFFICIAL.getCode(), 1);

        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(entity));
        when(medicalRecordRepository.findFirstByPatientIdAndStatusOrderByVersionNoDesc(
                PATIENT_ID, MedicalRecordStatus.OFFICIAL.getCode())).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(entity)).thenReturn(entity);
        when(converter.toResponse(entity)).thenReturn(response);

        Result<MedicalRecordResponse> result = service.publish(RECORD_ID, DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(MedicalRecordStatus.OFFICIAL.getCode(), entity.getStatus());
        assertEquals(1, entity.getVersionNo());
    }

    @Test
    void publish_shouldSetOfficialAndIncrementVersionWhenExistingOfficial() {
        MedicalRecordEntity entity = new MedicalRecordEntity();
        entity.setId(RECORD_ID);
        entity.setPatientId(PATIENT_ID);
        entity.setDoctorId(DOCTOR_USER_ID);
        entity.setStatus(MedicalRecordStatus.DRAFT.getCode());
        entity.setVersionNo(0);

        MedicalRecordEntity existingOfficial = new MedicalRecordEntity();
        existingOfficial.setVersionNo(3);

        MedicalRecordResponse response = buildResponse(RECORD_ID, MedicalRecordStatus.OFFICIAL.getCode(), 4);

        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(entity));
        when(medicalRecordRepository.findFirstByPatientIdAndStatusOrderByVersionNoDesc(
                PATIENT_ID, MedicalRecordStatus.OFFICIAL.getCode())).thenReturn(Optional.of(existingOfficial));
        when(medicalRecordRepository.save(entity)).thenReturn(entity);
        when(converter.toResponse(entity)).thenReturn(response);

        Result<MedicalRecordResponse> result = service.publish(RECORD_ID, DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(4, entity.getVersionNo());
    }

    // ---------- getById ----------

    @Test
    void getById_shouldReturnNotFoundWhenNotExists() {
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

        Result<MedicalRecordResponse> result = service.getById(RECORD_ID, DOCTOR_USER_ID);

        assertEquals(GlobalErrorCode.MEDICAL_RECORD_NOT_FOUND.getCode(), result.getCode());
    }

    @Test
    void getById_shouldReturnForbiddenWhenNotOwner() {
        MedicalRecordEntity entity = new MedicalRecordEntity();
        entity.setId(RECORD_ID);
        entity.setDoctorId(OTHER_DOCTOR_ID);
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(entity));

        Result<MedicalRecordResponse> result = service.getById(RECORD_ID, DOCTOR_USER_ID);

        assertEquals(GlobalErrorCode.FORBIDDEN.getCode(), result.getCode());
    }

    @Test
    void getById_shouldReturnResponseWhenExists() {
        MedicalRecordEntity entity = new MedicalRecordEntity();
        entity.setId(RECORD_ID);
        entity.setDoctorId(DOCTOR_USER_ID);
        MedicalRecordResponse response = buildResponse(RECORD_ID, MedicalRecordStatus.DRAFT.getCode(), 0);
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(entity));
        when(converter.toResponse(entity)).thenReturn(response);

        Result<MedicalRecordResponse> result = service.getById(RECORD_ID, DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertNotNull(result.getData());
    }

    // ---------- listByPatient ----------

    @Test
    void listByPatient_shouldReturnList() {
        MedicalRecordEntity entity = new MedicalRecordEntity();
        entity.setId(RECORD_ID);
        MedicalRecordResponse response = buildResponse(RECORD_ID, MedicalRecordStatus.OFFICIAL.getCode(), 1);
        when(medicalRecordRepository.findByPatientIdAndDoctorIdOrderByVersionNoDesc(PATIENT_ID, DOCTOR_USER_ID))
                .thenReturn(List.of(entity));
        when(converter.toResponse(entity)).thenReturn(response);

        Result<List<MedicalRecordResponse>> result = service.listByPatient(PATIENT_ID, DOCTOR_USER_ID);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(1, result.getData().size());
    }

    // ---------- listTemplatesByDepartment ----------

    @Test
    void listTemplatesByDepartment_shouldReturnAllEnabledWhenDepartmentIsNull() {
        MedicalRecordTemplateEntity template = new MedicalRecordTemplateEntity();
        template.setId(1L);
        MedicalRecordTemplateResponse response = new MedicalRecordTemplateResponse(
                1L, "内科", "初诊模板", "主诉", "现病史", "既往史", "诊断", "治疗方案", true, null);
        when(templateRepository.findByEnabled(true)).thenReturn(List.of(template));
        when(converter.toTemplateResponse(template)).thenReturn(response);

        Result<List<MedicalRecordTemplateResponse>> result = service.listTemplatesByDepartment(null);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(1, result.getData().size());
        verify(templateRepository).findByEnabled(true);
    }

    @Test
    void listTemplatesByDepartment_shouldReturnAllEnabledWhenDepartmentIsBlank() {
        MedicalRecordTemplateEntity template = new MedicalRecordTemplateEntity();
        template.setId(1L);
        MedicalRecordTemplateResponse response = new MedicalRecordTemplateResponse(
                1L, "内科", "初诊模板", "主诉", "现病史", "既往史", "诊断", "治疗方案", true, null);
        when(templateRepository.findByEnabled(true)).thenReturn(List.of(template));
        when(converter.toTemplateResponse(template)).thenReturn(response);

        Result<List<MedicalRecordTemplateResponse>> result = service.listTemplatesByDepartment("   ");

        assertEquals("SUCCESS", result.getCode());
        assertEquals(1, result.getData().size());
        verify(templateRepository).findByEnabled(true);
    }

    @Test
    void listTemplatesByDepartment_shouldReturnFilteredWhenDepartmentSpecified() {
        MedicalRecordTemplateEntity template = new MedicalRecordTemplateEntity();
        template.setId(1L);
        MedicalRecordTemplateResponse response = new MedicalRecordTemplateResponse(
                1L, "内科", "初诊模板", "主诉", "现病史", "既往史", "诊断", "治疗方案", true, null);
        when(templateRepository.findByDepartmentAndEnabled("内科", true)).thenReturn(List.of(template));
        when(converter.toTemplateResponse(template)).thenReturn(response);

        Result<List<MedicalRecordTemplateResponse>> result = service.listTemplatesByDepartment("内科");

        assertEquals("SUCCESS", result.getCode());
        assertEquals(1, result.getData().size());
        verify(templateRepository).findByDepartmentAndEnabled("内科", true);
    }

    // ---------- 测试数据构造 ----------

    private MedicalRecordCreateRequest buildCreateRequest(boolean publish) {
        return new MedicalRecordCreateRequest(
                PATIENT_ID,
                null,
                null,
                "头痛",
                "无",
                "无",
                "感冒",
                "对症治疗",
                "无",
                publish
        );
    }

    private MedicalRecordResponse buildResponse(Long id, String status, int versionNo) {
        return new MedicalRecordResponse(
                id, PATIENT_ID, DOCTOR_USER_ID, "内科",
                versionNo, status, "头痛", "无", "无",
                "感冒", "对症治疗", null, null, false, "无",
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
