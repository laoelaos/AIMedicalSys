package com.aimedical.modules.ai.impl.mock;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisRequest;
import com.aimedical.modules.ai.api.dto.discussion.DiscussionConclusionRequest;
import com.aimedical.modules.ai.api.dto.examination.ExaminationRecommendRequest;
import com.aimedical.modules.ai.api.dto.execution.ExecutionOrderRequest;
import com.aimedical.modules.ai.api.dto.image.ImageAnalysisRequest;
import com.aimedical.modules.ai.api.dto.inspection.InspectionReportRequest;
import com.aimedical.modules.ai.api.dto.kb.KbQueryRequest;
import com.aimedical.modules.ai.api.dto.labtest.LabTestReportRequest;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenRequest;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckRequest;
import com.aimedical.modules.ai.api.dto.schedule.ScheduleRequest;
import com.aimedical.modules.ai.api.dto.triage.TriageRequest;
import com.aimedical.modules.ai.api.dto.triage.TriageResponse;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;

class MockAiServiceTest {

    private final MockAiService service = new MockAiService();

    @Test
    void shouldBeAnnotatedWithService() {
        assertNotNull(MockAiService.class.getAnnotation(Service.class));
    }

    @Test
    void triageShouldReturnMockData() {
        var future = service.triage(new TriageRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
        var data = result.getData();
        assertNotNull(data.getRecommendedDepartments());
        assertEquals("mock_departmentName", data.getRecommendedDepartments().get(0).getDepartmentName());
        assertEquals("mock_reason", data.getReason());
    }

    @Test
    void diagnosisShouldReturnMockData() {
        var future = service.diagnosis(new DiagnosisRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }

    @Test
    void prescriptionCheckShouldReturnMockData() {
        var future = service.prescriptionCheck(new PrescriptionCheckRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }

    @Test
    void generateMedicalRecordShouldReturnMockData() {
        var future = service.generateMedicalRecord(new MedicalRecordGenRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }

    @Test
    void analysisReportForInspectionShouldReturnMockData() {
        var future = service.analysisReportForInspection(new InspectionReportRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }

    @Test
    void analysisReportForLabTestShouldReturnMockData() {
        var future = service.analysisReportForLabTest(new LabTestReportRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }

    @Test
    void imageAnalysisShouldReturnMockData() {
        var future = service.imageAnalysis(new ImageAnalysisRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }

    @Test
    void knowledgeBaseQueryShouldReturnMockData() {
        var future = service.knowledgeBaseQuery(new KbQueryRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }

    @Test
    void recommendExaminationShouldReturnMockData() {
        var future = service.recommendExamination(new ExaminationRecommendRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }

    @Test
    void prescriptionAssistShouldReturnMockData() {
        var future = service.prescriptionAssist(new PrescriptionAssistRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }

    @Test
    void recommendExecutionOrderShouldReturnMockData() {
        var future = service.recommendExecutionOrder(new ExecutionOrderRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }

    @Test
    void scheduleShouldReturnMockData() {
        var future = service.schedule(new ScheduleRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }

    @Test
    void discussionConclusionShouldReturnMockData() {
        var future = service.discussionConclusion(new DiscussionConclusionRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        var result = future.join();
        assertTrue(result.isSuccess());
        assertFalse(result.isDegraded());
        assertNotNull(result.getData());
    }
}
