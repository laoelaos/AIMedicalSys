package com.aimedical.modules.medicalrecord.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenRequest;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse;
import com.aimedical.modules.medicalrecord.dto.FieldMissingHint;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateRequest;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateResponse;
import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import com.aimedical.modules.medicalrecord.exception.MedicalRecordErrorCode;

@Component
public class MedicalRecordConverter {

    private final ObjectMapper objectMapper;

    public MedicalRecordConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<MedicalRecordField, String> toFieldsMap(MedicalRecordGenResponse aiResponse) {
        Map<MedicalRecordField, String> map = new HashMap<>();
        map.put(MedicalRecordField.CHIEF_COMPLAINT, aiResponse.getChiefComplaint());
        map.put(MedicalRecordField.SYMPTOM_DESCRIPTION, aiResponse.getSymptomDescription());
        map.put(MedicalRecordField.PRESENT_ILLNESS, aiResponse.getPresentIllness());
        map.put(MedicalRecordField.PAST_HISTORY, aiResponse.getPastHistory());
        map.put(MedicalRecordField.PHYSICAL_EXAM, aiResponse.getPhysicalExam());
        map.put(MedicalRecordField.PRELIMINARY_DIAGNOSIS, aiResponse.getPreliminaryDiagnosis());
        map.put(MedicalRecordField.TREATMENT_PLAN, aiResponse.getTreatmentPlan());
        return map;
    }

    public MedicalRecordGenRequest toAiRequest(RecordGenerateRequest request) {
        MedicalRecordGenRequest aiRequest = new MedicalRecordGenRequest();
        aiRequest.setDialogueText(request.getDialogueText());
        aiRequest.setPatientId(request.getPatientId());
        aiRequest.setEncounterId(request.getEncounterId());
        aiRequest.setStream(request.isStream());
        aiRequest.setDepartmentId(request.getDepartmentId());
        return aiRequest;
    }

    public RecordGenerateResponse toRecordGenerateResponse(
            AiResult<MedicalRecordGenResponse> aiResult, List<FieldMissingHint> hints) {
        RecordGenerateResponse response = new RecordGenerateResponse();
        if (aiResult.getData() != null) {
            response.setFields(toFieldsMap(aiResult.getData()));
        } else {
            response.setFields(new HashMap<>());
        }
        response.setMissingFieldHints(hints);
        response.setDegraded(aiResult.isDegraded());
        if (aiResult.getErrorCode() != null) {
            try {
                response.setErrorCode(MedicalRecordErrorCode.valueOf(aiResult.getErrorCode()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        boolean success = (aiResult.isSuccess() && aiResult.getData() != null)
                || MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.name().equals(aiResult.getErrorCode());
        response.setSuccess(success);
        return response;
    }
}
