package com.aimedical.modules.medicalrecord.converter;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenRequest;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse;
import com.aimedical.modules.medicalrecord.dto.FieldMissingHint;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateRequest;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateResponse;
import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import com.aimedical.modules.medicalrecord.exception.MedicalRecordErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordConverterTest {

    private MedicalRecordConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MedicalRecordConverter(new ObjectMapper());
    }

    @Test
    void toFieldsMapShouldMapSevenBusinessFields() {
        MedicalRecordGenResponse aiResp = new MedicalRecordGenResponse();
        aiResp.setChiefComplaint("头痛");
        aiResp.setSymptomDescription("跳痛");
        aiResp.setPresentIllness("3天");
        aiResp.setPastHistory("无");
        aiResp.setPhysicalExam("正常");
        aiResp.setPreliminaryDiagnosis("偏头痛");
        aiResp.setTreatmentPlan("休息");

        Map<MedicalRecordField, String> map = converter.toFieldsMap(aiResp);

        assertEquals(7, map.size());
        assertEquals("头痛", map.get(MedicalRecordField.CHIEF_COMPLAINT));
        assertEquals("跳痛", map.get(MedicalRecordField.SYMPTOM_DESCRIPTION));
        assertEquals("3天", map.get(MedicalRecordField.PRESENT_ILLNESS));
        assertEquals("无", map.get(MedicalRecordField.PAST_HISTORY));
        assertEquals("正常", map.get(MedicalRecordField.PHYSICAL_EXAM));
        assertEquals("偏头痛", map.get(MedicalRecordField.PRELIMINARY_DIAGNOSIS));
        assertEquals("休息", map.get(MedicalRecordField.TREATMENT_PLAN));
        assertNull(map.get(MedicalRecordField.MISSING_FIELDS));
        assertNull(map.get(MedicalRecordField.PARTIAL_CONTENT));
    }

    @Test
    void toFieldsMapShouldPreserveNullValues() {
        MedicalRecordGenResponse aiResp = new MedicalRecordGenResponse();
        Map<MedicalRecordField, String> map = converter.toFieldsMap(aiResp);
        assertEquals(7, map.size());
        assertNull(map.get(MedicalRecordField.CHIEF_COMPLAINT));
    }

    @Test
    void toAiRequestShouldCopyAllFields() {
        RecordGenerateRequest req = new RecordGenerateRequest();
        req.setDialogueText("对话内容");
        req.setPatientId("P001");
        req.setEncounterId("E001");
        req.setStream(true);
        req.setDepartmentId("dept-01");

        MedicalRecordGenRequest aiReq = converter.toAiRequest(req);

        assertEquals("对话内容", aiReq.getDialogueText());
        assertEquals("P001", aiReq.getPatientId());
        assertEquals("E001", aiReq.getEncounterId());
        assertTrue(aiReq.isStream());
        assertEquals("dept-01", aiReq.getDepartmentId());
    }

    @Test
    void toRecordGenerateResponseShouldBuildResponseFromAiResult() {
        MedicalRecordGenResponse aiResp = new MedicalRecordGenResponse();
        aiResp.setChiefComplaint("头痛");
        AiResult<MedicalRecordGenResponse> aiResult = AiResult.success(aiResp);
        List<FieldMissingHint> hints = Collections.emptyList();

        RecordGenerateResponse response = converter.toRecordGenerateResponse(aiResult, hints);

        assertTrue(response.isSuccess());
        assertFalse(response.isDegraded());
        assertNull(response.getErrorCode());
        assertSame(hints, response.getMissingFieldHints());
        assertEquals(7, response.getFields().size());
        assertEquals("头痛", response.getFields().get(MedicalRecordField.CHIEF_COMPLAINT));
    }

    @Test
    void toRecordGenerateResponseShouldReturnSuccessFalseWhenNonTimeoutFailure() {
        AiResult<MedicalRecordGenResponse> aiResult = AiResult.failure("ERROR");
        List<FieldMissingHint> hints = new ArrayList<>();

        RecordGenerateResponse response = converter.toRecordGenerateResponse(aiResult, hints);

        assertFalse(response.isSuccess());
        assertTrue(response.getFields().isEmpty());
    }

    @Test
    void toRecordGenerateResponseShouldSetTimeoutErrorCode() {
        MedicalRecordGenResponse aiResp = new MedicalRecordGenResponse();
        AiResult<MedicalRecordGenResponse> aiResult = AiResult.failure("MR_GEN_AI_TIMEOUT");
        aiResult.setData(aiResp);
        aiResult.setDegraded(true);

        RecordGenerateResponse response = converter.toRecordGenerateResponse(aiResult, Collections.emptyList());

        assertTrue(response.isSuccess());
        assertTrue(response.isDegraded());
        assertEquals(MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT, response.getErrorCode());
    }

    @Test
    void toRecordGenerateResponseShouldReturnSuccessFalseWhenSuccessWithNullData() {
        AiResult<MedicalRecordGenResponse> aiResult = new AiResult<>();
        aiResult.setSuccess(true);
        aiResult.setData(null);

        RecordGenerateResponse response = converter.toRecordGenerateResponse(aiResult, Collections.emptyList());

        assertFalse(response.isSuccess());
    }

    @Test
    void toRecordGenerateResponseShouldReturnSuccessTrueWhenTimeoutEvenWithNullData() {
        AiResult<MedicalRecordGenResponse> aiResult = AiResult.failure("MR_GEN_AI_TIMEOUT");
        aiResult.setDegraded(true);

        RecordGenerateResponse response = converter.toRecordGenerateResponse(aiResult, Collections.emptyList());

        assertTrue(response.isSuccess());
        assertTrue(response.isDegraded());
    }

    @Test
    void toRecordGenerateResponseShouldSetInterruptedErrorCode() {
        MedicalRecordGenResponse aiResp = new MedicalRecordGenResponse();
        aiResp.setChiefComplaint("头痛");
        AiResult<MedicalRecordGenResponse> aiResult = AiResult.failure("MR_GEN_AI_INTERRUPTED");
        aiResult.setData(aiResp);
        aiResult.setDegraded(true);

        RecordGenerateResponse response = converter.toRecordGenerateResponse(aiResult, Collections.emptyList());

        assertFalse(response.isSuccess());
        assertTrue(response.isDegraded());
        assertEquals(MedicalRecordErrorCode.MR_GEN_AI_INTERRUPTED, response.getErrorCode());
    }

    @Test
    void toRecordGenerateResponseShouldSetExecutionErrorCode() {
        MedicalRecordGenResponse aiResp = new MedicalRecordGenResponse();
        aiResp.setChiefComplaint("头痛");
        AiResult<MedicalRecordGenResponse> aiResult = AiResult.failure("MR_GEN_AI_EXECUTION_ERROR");
        aiResult.setData(aiResp);
        aiResult.setDegraded(true);

        RecordGenerateResponse response = converter.toRecordGenerateResponse(aiResult, Collections.emptyList());

        assertFalse(response.isSuccess());
        assertTrue(response.isDegraded());
        assertEquals(MedicalRecordErrorCode.MR_GEN_AI_EXECUTION_ERROR, response.getErrorCode());
    }

    @Test
    void toRecordGenerateResponseShouldIgnoreUnknownErrorCode() {
        MedicalRecordGenResponse aiResp = new MedicalRecordGenResponse();
        aiResp.setChiefComplaint("头痛");
        AiResult<MedicalRecordGenResponse> aiResult = AiResult.failure("SOME_UNKNOWN_ERROR");
        aiResult.setData(aiResp);
        aiResult.setDegraded(true);

        RecordGenerateResponse response = converter.toRecordGenerateResponse(aiResult, Collections.emptyList());

        assertFalse(response.isSuccess());
        assertTrue(response.isDegraded());
        assertNull(response.getErrorCode());
    }
}
