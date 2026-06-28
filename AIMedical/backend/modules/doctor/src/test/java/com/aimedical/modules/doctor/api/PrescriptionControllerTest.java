package com.aimedical.modules.doctor.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.CurrentUser;
import com.aimedical.modules.doctor.dto.request.PrescriptionAuditRequest;
import com.aimedical.modules.doctor.dto.request.PrescriptionCreateRequest;
import com.aimedical.modules.doctor.dto.request.PrescriptionItemRequest;
import com.aimedical.modules.doctor.dto.response.PrescriptionResponse;
import com.aimedical.modules.doctor.service.PrescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PrescriptionController} 单元测试。
 *
 * <p>验证控制器正确委托给 service 并传递当前医生 ID；
 * 覆盖 currentDoctorId() 空值保护分支。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionControllerTest {

    @Mock
    private PrescriptionService prescriptionService;

    @Mock
    private CurrentUser currentUser;

    private PrescriptionController controller;

    private static final Long DOCTOR_ID = 200L;
    private static final Long PRESCRIPTION_ID = 1L;
    private static final Long PATIENT_ID = 100L;

    @BeforeEach
    void setUp() {
        controller = new PrescriptionController(prescriptionService, currentUser);
    }

    private PrescriptionCreateRequest buildCreateRequest() {
        return new PrescriptionCreateRequest(
                PATIENT_ID, "上呼吸道感染", "饭后服用", false,
                List.of(new PrescriptionItemRequest(
                        "阿莫西林", "0.25g/粒", "1粒", "口服", "每日3次",
                        new BigDecimal("6"), "粒", "无"))
        );
    }

    private PrescriptionResponse buildResponse() {
        return new PrescriptionResponse(
                PRESCRIPTION_ID, PATIENT_ID, "张三", DOCTOR_ID, "内科",
                "DRAFT", "上呼吸道感染", false, null,
                null, null, "饭后服用",
                LocalDateTime.now(), LocalDateTime.now(), List.of()
        );
    }

    @Test
    void create_shouldDelegateToServiceWithCurrentDoctorId() {
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(prescriptionService.create(buildCreateRequest(), DOCTOR_ID)).thenReturn(Result.success(buildResponse()));

        Result<PrescriptionResponse> result = controller.create(buildCreateRequest());

        assertEquals("SUCCESS", result.getCode());
        verify(prescriptionService).create(buildCreateRequest(), DOCTOR_ID);
    }

    @Test
    void get_shouldDelegateToServiceWithId() {
        when(prescriptionService.getById(PRESCRIPTION_ID)).thenReturn(Result.success(buildResponse()));

        Result<PrescriptionResponse> result = controller.get(PRESCRIPTION_ID);

        assertEquals("SUCCESS", result.getCode());
        verify(prescriptionService).getById(PRESCRIPTION_ID);
    }

    @Test
    void listByPatient_shouldDelegateToServiceWithPatientId() {
        when(prescriptionService.listByPatient(PATIENT_ID)).thenReturn(Result.success(List.of(buildResponse())));

        Result<List<PrescriptionResponse>> result = controller.listByPatient(PATIENT_ID);

        assertEquals("SUCCESS", result.getCode());
        verify(prescriptionService).listByPatient(PATIENT_ID);
    }

    @Test
    void submitForReview_shouldDelegateToServiceWithIdAndCurrentDoctorId() {
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(prescriptionService.submitForReview(PRESCRIPTION_ID, DOCTOR_ID)).thenReturn(Result.success(buildResponse()));

        Result<PrescriptionResponse> result = controller.submitForReview(PRESCRIPTION_ID);

        assertEquals("SUCCESS", result.getCode());
        verify(prescriptionService).submitForReview(PRESCRIPTION_ID, DOCTOR_ID);
    }

    @Test
    void audit_shouldDelegateToServiceWithIdRequestAndCurrentDoctorId() {
        PrescriptionAuditRequest auditRequest = new PrescriptionAuditRequest(true, "审核通过");
        when(currentUser.getUserId()).thenReturn(DOCTOR_ID);
        when(prescriptionService.audit(PRESCRIPTION_ID, auditRequest, DOCTOR_ID)).thenReturn(Result.success(buildResponse()));

        Result<PrescriptionResponse> result = controller.audit(PRESCRIPTION_ID, auditRequest);

        assertEquals("SUCCESS", result.getCode());
        verify(prescriptionService).audit(PRESCRIPTION_ID, auditRequest, DOCTOR_ID);
    }

    @Test
    void anyEndpoint_shouldThrowWhenUserIdIsNull() {
        when(currentUser.getUserId()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> controller.submitForReview(PRESCRIPTION_ID));
    }
}
