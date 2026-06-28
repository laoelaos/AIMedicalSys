package com.aimedical.modules.doctor.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.CurrentUser;
import com.aimedical.modules.doctor.dto.request.MedicalRecordCreateRequest;
import com.aimedical.modules.doctor.dto.response.MedicalRecordResponse;
import com.aimedical.modules.doctor.dto.response.MedicalRecordTemplateResponse;
import com.aimedical.modules.doctor.service.MedicalRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MedicalRecordController} 单元测试。
 *
 * <p>验证控制器正确委托给 service 并传递当前医生 ID；
 * 覆盖 currentDoctorId() 空值保护分支。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class MedicalRecordControllerTest {

    @Mock
    private MedicalRecordService medicalRecordService;

    @Mock
    private CurrentUser currentUser;

    private MedicalRecordController controller;

    private static final Long DOCTOR_ID = 200L;
    private static final Long RECORD_ID = 1L;
    private static final Long PATIENT_ID = 100L;

    @BeforeEach
    void setUp() {
        controller = new MedicalRecordController(medicalRecordService, currentUser);
    }

    private MedicalRecordCreateRequest buildCreateRequest() {
        return new MedicalRecordCreateRequest(
                PATIENT_ID, null, null,
                "头痛", "无", "无", "感冒", "对症治疗", "无", false
        );
    }

    private MedicalRecordResponse buildResponse() {
        return new MedicalRecordResponse(
                RECORD_ID, PATIENT_ID, DOCTOR_ID, "内科",
                0, "DRAFT", "头痛", "无", "无",
                "感冒", "对症治疗", null, null, false, "无",
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void create_shouldDelegateToServiceWithCurrentDoctorId() {
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(medicalRecordService.createOrUpdateDraft(buildCreateRequest(), DOCTOR_ID))
                .thenReturn(Result.success(buildResponse()));

        Result<MedicalRecordResponse> result = controller.create(buildCreateRequest());

        assertEquals("SUCCESS", result.getCode());
        verify(medicalRecordService).createOrUpdateDraft(buildCreateRequest(), DOCTOR_ID);
    }

    @Test
    void get_shouldDelegateToServiceWithId() {
        when(medicalRecordService.getById(RECORD_ID)).thenReturn(Result.success(buildResponse()));

        Result<MedicalRecordResponse> result = controller.get(RECORD_ID);

        assertEquals("SUCCESS", result.getCode());
        verify(medicalRecordService).getById(RECORD_ID);
    }

    @Test
    void listByPatient_shouldDelegateToServiceWithPatientId() {
        when(medicalRecordService.listByPatient(PATIENT_ID)).thenReturn(Result.success(List.of(buildResponse())));

        Result<List<MedicalRecordResponse>> result = controller.listByPatient(PATIENT_ID);

        assertEquals("SUCCESS", result.getCode());
        verify(medicalRecordService).listByPatient(PATIENT_ID);
    }

    @Test
    void publish_shouldDelegateToServiceWithIdAndCurrentDoctorId() {
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(medicalRecordService.publish(RECORD_ID, DOCTOR_ID)).thenReturn(Result.success(buildResponse()));

        Result<MedicalRecordResponse> result = controller.publish(RECORD_ID);

        assertEquals("SUCCESS", result.getCode());
        verify(medicalRecordService).publish(RECORD_ID, DOCTOR_ID);
    }

    @Test
    void listTemplates_shouldDelegateToServiceWithDepartment() {
        MedicalRecordTemplateResponse templateResponse = new MedicalRecordTemplateResponse(
                1L, "内科", "初诊模板", "主诉", "现病史", "既往史", "诊断", "治疗方案", true, null);
        when(medicalRecordService.listTemplatesByDepartment("内科"))
                .thenReturn(Result.success(List.of(templateResponse)));

        Result<List<MedicalRecordTemplateResponse>> result = controller.listTemplates("内科");

        assertEquals("SUCCESS", result.getCode());
        verify(medicalRecordService).listTemplatesByDepartment("内科");
    }

    @Test
    void listTemplates_shouldDelegateToServiceWithNullDepartment() {
        when(medicalRecordService.listTemplatesByDepartment(null)).thenReturn(Result.success(List.of()));

        Result<List<MedicalRecordTemplateResponse>> result = controller.listTemplates(null);

        assertEquals("SUCCESS", result.getCode());
        verify(medicalRecordService).listTemplatesByDepartment(null);
    }

    @Test
    void anyEndpoint_shouldThrowWhenUserIdIsNull() {
        when(currentUser.getUserId()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> controller.publish(RECORD_ID));
    }
}
