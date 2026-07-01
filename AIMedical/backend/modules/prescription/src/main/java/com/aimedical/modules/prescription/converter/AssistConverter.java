package com.aimedical.modules.prescription.converter;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.dto.prescription.AllergyDetailItem;
import com.aimedical.modules.ai.api.dto.prescription.AllergyWarningItem;
import com.aimedical.modules.ai.api.dto.prescription.DoseWarningItem;
import com.aimedical.modules.ai.api.dto.prescription.PatientInfo;
import com.aimedical.modules.prescription.dto.assist.AllergyWarningSeverity;
import com.aimedical.modules.prescription.dto.assist.DosageAlertLevel;
import com.aimedical.modules.prescription.dto.assist.DoseWarning;
import com.aimedical.modules.prescription.dto.assist.DoseWarningType;
import com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequest;
import com.aimedical.modules.prescription.dto.assist.PrescriptionAssistResponse;
import com.aimedical.modules.prescription.dto.audit.AllergyDetail;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AssistConverter {

    public com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest toAiPrescriptionAssistRequest(
            PrescriptionAssistRequest request) {
        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest aiRequest =
                new com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest();
        aiRequest.setDiagnosis(request.getDiagnosis());
        aiRequest.setExamResults(request.getExamResults());
        aiRequest.setExistingPrescription(request.getExistingPrescription());
        aiRequest.setPrescriptionId(request.getPrescriptionId());
        aiRequest.setEncounterId(request.getEncounterId());

        if (request.getPatientInfo() != null) {
            aiRequest.setPatientInfo(toAiPatientInfo(request.getPatientInfo()));
        }

        return aiRequest;
    }

    public PrescriptionAssistResponse toPrescriptionAssistResponse(
            AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult) {
        PrescriptionAssistResponse response = new PrescriptionAssistResponse();

        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse aiData = aiResult.getData();
        if (!aiResult.isSuccess() || aiData == null) {
            response.setPrescriptionDraft("");
            response.setDoseWarnings(Collections.emptyList());
            response.setAllergyWarnings(Collections.emptyList());
            response.setDisclaimerRequired(true);
            return response;
        }

        response.setPrescriptionDraft(aiData.getPrescriptionDraft());
        response.setDoseWarnings(mapDoseWarnings(aiData.getDoseWarnings()));
        response.setAllergyWarnings(mapAllergyWarnings(aiData.getAllergyWarnings()));
        response.setDisclaimerRequired(aiData.isDisclaimerRequired());
        response.setErrorCode(aiData.getErrorCode());

        return response;
    }

    private PatientInfo toAiPatientInfo(com.aimedical.modules.prescription.dto.audit.PatientInfo bizPatient) {
        PatientInfo aiPatient = new PatientInfo();
        aiPatient.setPatientId(bizPatient.getPatientId());
        aiPatient.setAge(bizPatient.getAge());
        aiPatient.setGender(bizPatient.getGender());
        aiPatient.setAllergyHistory(bizPatient.getAllergyHistory());
        aiPatient.setComorbidities(bizPatient.getComorbidities());

        if (bizPatient.getAllergyDetails() != null) {
            aiPatient.setAllergyDetails(bizPatient.getAllergyDetails().stream()
                    .map(this::toAiAllergyDetail)
                    .collect(Collectors.toList()));
        }

        return aiPatient;
    }

    private AllergyDetailItem toAiAllergyDetail(AllergyDetail detail) {
        AllergyDetailItem item = new AllergyDetailItem();
        item.setAllergen(detail.getAllergen());
        item.setReactionType(detail.getReactionType());
        item.setSeverity(detail.getSeverity() != null ? detail.getSeverity().name() : null);
        item.setOccurredAt(detail.getOccurredAt());
        return item;
    }

    private List<DoseWarning> mapDoseWarnings(List<DoseWarningItem> items) {
        if (items == null) return Collections.emptyList();
        return items.stream().map(i -> {
            DoseWarning dw = new DoseWarning();
            dw.setDrugId(i.getDrugId());
            dw.setWarningType(mapDoseWarningType(i.getWarningType()));
            dw.setMessage(i.getMessage());
            dw.setSeverity(mapDosageAlertLevel(i.getSeverity()));
            return dw;
        }).collect(Collectors.toList());
    }

    private List<com.aimedical.modules.prescription.dto.assist.AllergyWarningItem> mapAllergyWarnings(
            List<AllergyWarningItem> items) {
        if (items == null) return Collections.emptyList();
        return items.stream().map(i -> {
            com.aimedical.modules.prescription.dto.assist.AllergyWarningItem aw =
                    new com.aimedical.modules.prescription.dto.assist.AllergyWarningItem();
            aw.setDrugId(i.getDrugId());
            aw.setAllergen(i.getAllergen());
            aw.setSeverity(mapAllergyWarningSeverity(i.getSeverity()));
            return aw;
        }).collect(Collectors.toList());
    }

    private DoseWarningType mapDoseWarningType(String warningType) {
        if (warningType == null) return DoseWarningType.OVER_SINGLE_DOSE;
        try {
            return DoseWarningType.valueOf(warningType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DoseWarningType.OVER_SINGLE_DOSE;
        }
    }

    private DosageAlertLevel mapDosageAlertLevel(String severity) {
        if (severity == null) return DosageAlertLevel.INFO;
        try {
            return DosageAlertLevel.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DosageAlertLevel.INFO;
        }
    }

    private AllergyWarningSeverity mapAllergyWarningSeverity(String severity) {
        if (severity == null) return AllergyWarningSeverity.INFO;
        try {
            return AllergyWarningSeverity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AllergyWarningSeverity.INFO;
        }
    }
}
