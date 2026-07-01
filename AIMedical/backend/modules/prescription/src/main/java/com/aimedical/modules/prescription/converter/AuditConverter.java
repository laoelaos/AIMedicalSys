package com.aimedical.modules.prescription.converter;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.dto.prescription.AlertItem;
import com.aimedical.modules.ai.api.dto.prescription.AllergyDetailItem;
import com.aimedical.modules.ai.api.dto.prescription.DrugInteractionItem;
import com.aimedical.modules.ai.api.dto.prescription.PatientInfo;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckItem;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckRequest;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckResponse;
import com.aimedical.modules.ai.api.dto.prescription.SuggestionItem;
import com.aimedical.modules.prescription.dto.audit.AllergyDetail;
import com.aimedical.modules.prescription.dto.audit.AuditAlert;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.AuditResponse;
import com.aimedical.modules.prescription.dto.audit.AlertSeverity;
import com.aimedical.modules.prescription.dto.audit.DrugInteraction;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.dto.audit.Suggestion;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditConverter {

    public PrescriptionCheckRequest toAiPrescriptionCheckRequest(AuditRequest request) {
        PrescriptionCheckRequest aiRequest = new PrescriptionCheckRequest();
        aiRequest.setPrescriptionId(request.getPrescriptionId());

        if (request.getPrescriptionItems() != null) {
            aiRequest.setPrescriptionItems(request.getPrescriptionItems().stream()
                    .map(this::toAiCheckItem)
                    .collect(Collectors.toList()));
        }

        if (request.getPatientInfo() != null) {
            aiRequest.setPatientInfo(toAiPatientInfo(request.getPatientInfo()));
        }

        return aiRequest;
    }

    public AuditResponse toAuditResponse(AiResult<PrescriptionCheckResponse> aiResult) {
        AuditResponse response = new AuditResponse();
        PrescriptionCheckResponse aiData = aiResult.getData();
        if (aiData == null) {
            response.setRiskLevel(AuditRiskLevel.PASS);
            response.setAlerts(Collections.emptyList());
            response.setInteractions(Collections.emptyList());
            response.setSuggestions(Collections.emptyList());
            response.setFromFallback(aiResult.isDegraded());
            return response;
        }

        response.setRiskLevel(mapRiskLevel(aiData.getRiskLevel()));
        response.setAlerts(mapAlerts(aiData.getAlerts()));
        response.setInteractions(mapInteractions(aiData.getInteractions()));
        response.setSuggestions(mapSuggestions(aiData.getSuggestions()));
        response.setFromFallback(aiData.isFromFallback());
        return response;
    }

    private PrescriptionCheckItem toAiCheckItem(PrescriptionItem item) {
        PrescriptionCheckItem aiItem = new PrescriptionCheckItem();
        aiItem.setDrugId(item.getDrugId());
        aiItem.setDrugName(item.getDrugName());
        aiItem.setDose(item.getDose().doubleValue());
        aiItem.setFrequency(item.getFrequency());
        aiItem.setDuration(item.getDuration());
        aiItem.setRoute(item.getRoute());
        aiItem.setUnit(item.getUnit());
        return aiItem;
    }

    private PatientInfo toAiPatientInfo(com.aimedical.modules.prescription.dto.audit.PatientInfo bizPatient) {
        PatientInfo aiPatient = new PatientInfo();
        aiPatient.setPatientId(bizPatient.getPatientId());
        aiPatient.setAge(bizPatient.getAge());
        aiPatient.setGender(bizPatient.getGender());
        aiPatient.setAllergyHistory(bizPatient.getAllergyHistory());
        aiPatient.setComorbidities(bizPatient.getComorbidities());
        aiPatient.setWeight(bizPatient.getWeight());

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

    private AuditRiskLevel mapRiskLevel(String aiRiskLevel) {
        if (aiRiskLevel == null) return AuditRiskLevel.PASS;
        switch (aiRiskLevel.toUpperCase()) {
            case "LOW": return AuditRiskLevel.PASS;
            case "MEDIUM": return AuditRiskLevel.WARN;
            case "HIGH": return AuditRiskLevel.BLOCK;
            default: return AuditRiskLevel.PASS;
        }
    }

    private List<AuditAlert> mapAlerts(List<AlertItem> alertItems) {
        if (alertItems == null) return Collections.emptyList();
        return alertItems.stream().map(a -> {
            AuditAlert alert = new AuditAlert();
            alert.setAlertCode(a.getAlertCode());
            alert.setAlertMessage(a.getAlertMessage());
            alert.setSeverity(mapAlertSeverity(a.getSeverity()));
            return alert;
        }).collect(Collectors.toList());
    }

    private AlertSeverity mapAlertSeverity(String severity) {
        if (severity == null) return AlertSeverity.INFO;
        try {
            return AlertSeverity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AlertSeverity.INFO;
        }
    }

    private List<DrugInteraction> mapInteractions(List<DrugInteractionItem> interactionItems) {
        if (interactionItems == null) return Collections.emptyList();
        return interactionItems.stream().map(i -> {
            DrugInteraction di = new DrugInteraction();
            di.setDrugPair(i.getDrugPair());
            di.setSeverity(i.getSeverity());
            di.setDescription(i.getDescription());
            return di;
        }).collect(Collectors.toList());
    }

    private List<Suggestion> mapSuggestions(List<SuggestionItem> suggestionItems) {
        if (suggestionItems == null) return Collections.emptyList();
        return suggestionItems.stream().map(s -> {
            Suggestion suggestion = new Suggestion();
            suggestion.setSuggestionCode(s.getSuggestionCode());
            suggestion.setSuggestionText(s.getSuggestionText());
            return suggestion;
        }).collect(Collectors.toList());
    }
}
