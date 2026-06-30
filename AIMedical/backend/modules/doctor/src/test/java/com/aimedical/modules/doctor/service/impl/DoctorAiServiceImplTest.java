package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.result.Result;
import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisRequest;
import com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisResponse;
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
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DoctorAiServiceImpl} 单元测试。
 *
 * <p>覆盖两条路径：
 * <ul>
 *   <li>{@code mockDegrade=true}（默认）验证降级兜底数据与字段</li>
 *   <li>{@code mockDegrade=false} 验证 AiService 成功/异常/降级三种分支的字段映射与降级行为</li>
 * </ul>
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
        // 降级路径下 riskLevel 为 null（表示未知风险，由人工判断），passed=false
        assertNull(result.getData().data().riskLevel());
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

    // ---------- mockDegrade=false 路径：覆盖 AiService 成功/异常/降级三种分支 ----------

    /**
     * 辅助构造一个 {@code mockDegrade=false} 的 DoctorAiServiceImpl 实例，
     * 用于验证非降级模式下的真实 AiService 调用路径。
     */
    private DoctorAiService nonDegradingService() {
        DoctorAiServiceImpl impl = new DoctorAiServiceImpl(aiService);
        ReflectionTestUtils.setField(impl, "mockDegrade", false);
        return impl;
    }

    /**
     * 成功分支：AiService 返回 {@link AiResult#success}，
     * 验证 {@code possibleDiagnoses} 与 {@code summary} 字段被正确映射到响应 DTO。
     */
    @Test
    void diagnosis_shouldMapAiDataWhenServiceSucceeds() {
        DiagnosisResponse aiData = new DiagnosisResponse();
        aiData.setPossibleDiagnoses(List.of("上呼吸道感染", "急性咽炎"));
        aiData.setSummary("结合主诉与体征，考虑上呼吸道感染可能性大");
        when(aiService.diagnosis(any(DiagnosisRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(AiResult.success(aiData)));

        DoctorAiService nonDegrading = nonDegradingService();
        Result<AiResultResponse<AiDiagnosisResponse>> result =
                nonDegrading.diagnosis(new AiDiagnosisRequest(100L, "头痛", "无", "无"), 200L);

        assertEquals("SUCCESS", result.getCode());
        assertNotNull(result.getData());
        assertFalse(result.getData().degraded());
        AiDiagnosisResponse data = result.getData().data();
        assertNotNull(data);
        assertEquals(List.of("上呼吸道感染", "急性咽炎"), data.possibleDiagnoses());
        assertEquals("结合主诉与体征，考虑上呼吸道感染可能性大", data.summary());
        verify(aiService).diagnosis(any(DiagnosisRequest.class));
    }

    /**
     * 异常分支：AiService 抛出异常（CompletableFuture.failedFuture），
     * 验证被 catch 后走降级路径返回兜底数据与降级原因。
     */
    @Test
    void diagnosis_shouldDegradeWhenServiceThrows() {
        when(aiService.diagnosis(any(DiagnosisRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("AI 服务不可用")));

        DoctorAiService nonDegrading = nonDegradingService();
        Result<AiResultResponse<AiDiagnosisResponse>> result =
                nonDegrading.diagnosis(new AiDiagnosisRequest(100L, "头痛", "无", "无"), 200L);

        assertEquals("SUCCESS", result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().degraded());
        assertNotNull(result.getData().fallbackReason());
        assertNotNull(result.getData().data());
        verify(aiService).diagnosis(any(DiagnosisRequest.class));
    }

    /**
     * 降级分支：AiService 自身返回 {@link AiResult#degraded}（如限流/熔断），
     * 验证 DoctorAiServiceImpl 将其转换为统一的降级响应。
     */
    @Test
    void diagnosis_shouldDegradeWhenServiceReturnsDegradedResult() {
        when(aiService.diagnosis(any(DiagnosisRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(AiResult.degraded("AI 服务限流，已降级")));

        DoctorAiService nonDegrading = nonDegradingService();
        Result<AiResultResponse<AiDiagnosisResponse>> result =
                nonDegrading.diagnosis(new AiDiagnosisRequest(100L, "头痛", "无", "无"), 200L);

        assertEquals("SUCCESS", result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().degraded());
        assertNotNull(result.getData().fallbackReason());
        verify(aiService).diagnosis(any(DiagnosisRequest.class));
    }
}
