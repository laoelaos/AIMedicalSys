package com.aimedical.modules.doctor.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.CurrentUser;
import com.aimedical.modules.doctor.dto.request.AiDiagnosisRequest;
import com.aimedical.modules.doctor.dto.request.AiExaminationRequest;
import com.aimedical.modules.doctor.dto.request.AiMedicalRecordGenRequest;
import com.aimedical.modules.doctor.dto.request.AiPrescriptionAssistRequest;
import com.aimedical.modules.doctor.dto.request.AiPrescriptionAuditRequest;
import com.aimedical.modules.doctor.dto.response.AiDiagnosisResponse;
import com.aimedical.modules.doctor.dto.response.AiExaminationResponse;
import com.aimedical.modules.doctor.dto.response.AiMedicalRecordGenResponse;
import com.aimedical.modules.doctor.dto.response.AiPrescriptionAssistResponse;
import com.aimedical.modules.doctor.dto.response.AiPrescriptionAuditResponse;
import com.aimedical.modules.doctor.dto.response.AiResultResponse;
import com.aimedical.modules.doctor.entity.AiRiskLevel;
import com.aimedical.modules.doctor.service.DoctorAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DoctorAiController} 单元测试。
 *
 * <p>验证控制器正确委托给 service 并传递当前医生 ID；
 * 覆盖 currentDoctorId() 空值保护分支。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DoctorAiControllerTest {

    @Mock
    private DoctorAiService doctorAiService;

    @Mock
    private CurrentUser currentUser;

    private DoctorAiController controller;

    private static final Long DOCTOR_ID = 200L;

    @BeforeEach
    void setUp() {
        controller = new DoctorAiController(doctorAiService, currentUser);
    }

    @Test
    void diagnosis_shouldDelegateToServiceWithCurrentDoctorId() {
        AiDiagnosisRequest request = new AiDiagnosisRequest(100L, "头痛", "无", "无");
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(doctorAiService.diagnosis(request, DOCTOR_ID))
                .thenReturn(Result.success(AiResultResponse.ok(new AiDiagnosisResponse(List.of(), "建议"))));

        Result<AiResultResponse<AiDiagnosisResponse>> result = controller.diagnosis(request);

        assertEquals("SUCCESS", result.getCode());
        verify(doctorAiService).diagnosis(request, DOCTOR_ID);
    }

    @Test
    void recommendExamination_shouldDelegateToServiceWithCurrentDoctorId() {
        AiExaminationRequest request = new AiExaminationRequest(100L, "感冒", "头痛");
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(doctorAiService.recommendExamination(request, DOCTOR_ID))
                .thenReturn(Result.success(AiResultResponse.ok(new AiExaminationResponse(List.of()))));

        Result<AiResultResponse<AiExaminationResponse>> result = controller.recommendExamination(request);

        assertEquals("SUCCESS", result.getCode());
        verify(doctorAiService).recommendExamination(request, DOCTOR_ID);
    }

    @Test
    void prescriptionAssist_shouldDelegateToServiceWithCurrentDoctorId() {
        AiPrescriptionAssistRequest request = new AiPrescriptionAssistRequest(100L, "感冒", "头痛");
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(doctorAiService.prescriptionAssist(request, DOCTOR_ID))
                .thenReturn(Result.success(AiResultResponse.ok(new AiPrescriptionAssistResponse(List.of(), "建议"))));

        Result<AiResultResponse<AiPrescriptionAssistResponse>> result = controller.prescriptionAssist(request);

        assertEquals("SUCCESS", result.getCode());
        verify(doctorAiService).prescriptionAssist(request, DOCTOR_ID);
    }

    @Test
    void prescriptionAudit_shouldDelegateToServiceWithCurrentDoctorId() {
        AiPrescriptionAuditRequest request = new AiPrescriptionAuditRequest(1L, "感冒", List.of("阿莫西林"));
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(doctorAiService.prescriptionAudit(request, DOCTOR_ID))
                .thenReturn(Result.success(AiResultResponse.ok(new AiPrescriptionAuditResponse(AiRiskLevel.LOW, List.of(), true))));

        Result<AiResultResponse<AiPrescriptionAuditResponse>> result = controller.prescriptionAudit(request);

        assertEquals("SUCCESS", result.getCode());
        verify(doctorAiService).prescriptionAudit(request, DOCTOR_ID);
    }

    @Test
    void generateMedicalRecord_shouldDelegateToServiceWithCurrentDoctorId() {
        AiMedicalRecordGenRequest request = new AiMedicalRecordGenRequest(100L, 1L, "头痛", "无", "无", "感冒");
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(doctorAiService.generateMedicalRecord(request, DOCTOR_ID))
                .thenReturn(Result.success(AiResultResponse.ok(new AiMedicalRecordGenResponse("", "", "", "", ""))));

        Result<AiResultResponse<AiMedicalRecordGenResponse>> result = controller.generateMedicalRecord(request);

        assertEquals("SUCCESS", result.getCode());
        verify(doctorAiService).generateMedicalRecord(request, DOCTOR_ID);
    }

    @Test
    void anyEndpoint_shouldThrowWhenUserIdIsNull() {
        when(currentUser.getUserId()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> controller.diagnosis(
                new AiDiagnosisRequest(100L, "头痛", "无", "无")));
    }
}
