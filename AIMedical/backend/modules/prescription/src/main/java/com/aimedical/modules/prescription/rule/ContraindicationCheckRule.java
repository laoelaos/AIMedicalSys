package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.repository.DrugContraindicationMappingRepository;
import com.aimedical.modules.prescription.rule.entity.DrugContraindicationMapping;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ContraindicationCheckRule {

    private static final String RULE_ID = "CONTRAINDICATION_CHECK";

    private final DrugContraindicationMappingRepository drugContraindicationMappingRepository;

    public ContraindicationCheckRule(DrugContraindicationMappingRepository drugContraindicationMappingRepository) {
        this.drugContraindicationMappingRepository = drugContraindicationMappingRepository;
    }

    public LocalRuleResult check(AuditRequest request) {
        PatientInfo patientInfo = request.getPatientInfo();
        if (patientInfo == null || patientInfo.getComorbidities() == null || patientInfo.getComorbidities().isEmpty()) {
            return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
        }

        for (PrescriptionItem item : request.getPrescriptionItems()) {
            Optional<DrugContraindicationMapping> mappingOpt = drugContraindicationMappingRepository.findByDrugCode(item.getDrugId());
            if (mappingOpt.isEmpty()) {
                continue;
            }
            DrugContraindicationMapping mapping = mappingOpt.get();
            List<Map<String, String>> contraindications = parseContraindications(mapping.getContraindications());

            for (Map<String, String> ci : contraindications) {
                String diseaseName = ci.get("diseaseName");
                String level = ci.get("level");
                if (diseaseName != null && patientInfo.getComorbidities().stream().anyMatch(c -> c.equalsIgnoreCase(diseaseName))) {
                    if ("ABSOLUTE_CONTRAINDICATION".equals(level)) {
                        return new LocalRuleResult(RULE_ID, false, "Absolute contraindication: " + diseaseName + " for drug " + item.getDrugId(), AuditRiskLevel.BLOCK);
                    } else {
                        return new LocalRuleResult(RULE_ID, false, "Relative contraindication: " + diseaseName + " for drug " + item.getDrugId(), AuditRiskLevel.WARN);
                    }
                }
            }
        }

        return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
    }

    private List<Map<String, String>> parseContraindications(String contraindicationsJson) {
        if (contraindicationsJson == null || contraindicationsJson.isBlank()) {
            return List.of();
        }
        try {
            com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>> typeRef = new com.fasterxml.jackson.core.type.TypeReference<>() {};
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(contraindicationsJson, typeRef);
        } catch (Exception e) {
            return List.of();
        }
    }
}
