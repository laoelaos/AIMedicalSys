package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.result.Result;
import com.aimedical.modules.ai.api.AiService;
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
import com.aimedical.modules.doctor.service.DoctorAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DoctorAiServiceImpl} 单元测试。
 *
 * <p>启用 {@code mockDegrade=true} 验证降级路径返回的兜底数据与字段；
 * 实际 AiService 调用路径由集成测试覆盖（CI 环境运行）。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DoctorAiServiceImplTest {

    @Mock
    private AiService aiService;

    private DoctorAiService service;

    @BeforeEach
    void setUp() {
        DoctorAiServiceImpl impl = new DoctorAiServiceImpl(aiService);
        // mockDegrade=true 为默认值，启用降级模式不实际调用 AiService
        ReflectionTestUtils.setField(impl, "mockDegrade", true);
        this.service = impl;
    }

    @Test
    void diagnosis_shouldReturnDegradedResultWhenMockDegrade() {
        Result<AiResultResponse<AiDiagnosisResponse>> result =
                service.diagnosis(new AiDiagnosisRequest(100L, "头痛", "无", "无"), 200L);

        assertEquals("SUCCESS", result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().degraded());
        assertNotNull(result.getData().fallbackReason());
        assertNotNull(result.getData().data());
        assertNotNull(result.getData().data().possibleDiagnoses());
    }

    @Test
    void recommendExamination_shouldReturnDegradedResultWithFallbackItems() {
        Result<AiResultResponse<AiExaminationResponse>> result =
                service.recommendExamination(new AiExaminationRequest(100L, "感冒", "头痛"), 200L);

        assertEquals("SUCCESS", result.getCode());
        assertTrue(result.getData().degraded());
        assertNotNull(result.getData().data());
        assertFalse(result.getData().data().items().isEmpty());
    }

    @Test
    void prescriptionAssist_shouldReturnDegradedResult() {
        Result<AiResultResponse<AiPrescriptionAssistResponse>> result =
                service.prescriptionAssist(
                        new AiPrescriptionAssistRequest(100L, "感冒", "头痛"), 200L);

        assertEquals("SUCCESS", result.getCode());
        assertTrue(result.getData().degraded());
        assertNotNull(result.getData().fallbackReason());
        assertNotNull(result.getData().data());
    }

    @Test
    void prescriptionAudit_shouldReturnDegradedResult() {
        Result<AiResultResponse<AiPrescriptionAuditResponse>> result =
                service.prescriptionAudit(
                        new AiPrescriptionAuditRequest(1L, "感冒", List.of("阿莫西林")), 200L);

        assertEquals("SUCCESS", result.getCode());
        assertTrue(result.getData().degraded());
        assertEquals("UNKNOWN", result.getData().data().riskLevel());
        assertFalse(result.getData().data().passed());
    }

    @Test
    void generateMedicalRecord_shouldReturnDegradedResult() {
        Result<AiResultResponse<AiMedicalRecordGenResponse>> result =
                service.generateMedicalRecord(
                        new AiMedicalRecordGenRequest(100L, 1L, "头痛", "无", "无", "感冒"), 200L);

        assertEquals("SUCCESS", result.getCode());
        assertTrue(result.getData().degraded());
        assertNotNull(result.getData().fallbackReason());
        assertNotNull(result.getData().data());
    }
}
