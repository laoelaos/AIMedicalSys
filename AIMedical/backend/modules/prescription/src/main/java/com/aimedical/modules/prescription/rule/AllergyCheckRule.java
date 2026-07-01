package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.patient.entity.AllergySeverity;
import com.aimedical.modules.prescription.dto.audit.AllergyDetail;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.repository.DrugAllergyMappingRepository;
import com.aimedical.modules.prescription.rule.entity.DrugAllergyMapping;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AllergyCheckRule {

    private static final String RULE_ID = "ALLERGY_CHECK";

    private final DrugAllergyMappingRepository drugAllergyMappingRepository;

    public AllergyCheckRule(DrugAllergyMappingRepository drugAllergyMappingRepository) {
        this.drugAllergyMappingRepository = drugAllergyMappingRepository;
    }

    public LocalRuleResult check(AuditRequest request) {
        PatientInfo patientInfo = request.getPatientInfo();
        if (patientInfo == null) {
            return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
        }

        List<AllergyDetail> allergyDetails = patientInfo.getAllergyDetails();
        String allergyHistory = patientInfo.getAllergyHistory();

        for (PrescriptionItem item : request.getPrescriptionItems()) {
            Optional<DrugAllergyMapping> mappingOpt = drugAllergyMappingRepository.findByDrugCode(item.getDrugId());
            if (mappingOpt.isEmpty()) {
                continue;
            }
            DrugAllergyMapping mapping = mappingOpt.get();
            List<String> allergens = parseAllergens(mapping.getAllergens());

            if (allergyDetails != null && !allergyDetails.isEmpty()) {
                for (AllergyDetail detail : allergyDetails) {
                    if (allergens.contains(detail.getAllergen())) {
                        AllergySeverity sev = detail.getSeverity();
                        if (sev == AllergySeverity.SEVERE) {
                            return new LocalRuleResult(RULE_ID, false, "Severe allergy to " + detail.getAllergen() + " for drug " + item.getDrugId(), AuditRiskLevel.BLOCK);
                        } else {
                            return new LocalRuleResult(RULE_ID, false, "Allergy to " + detail.getAllergen() + " for drug " + item.getDrugId(), AuditRiskLevel.WARN);
                        }
                    }
                }
            } else if (allergyHistory != null && !allergyHistory.isBlank()) {
                for (String allergen : allergens) {
                    Pattern pattern = Pattern.compile("\\b" + Pattern.quote(allergen.toLowerCase()) + "\\b", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(allergyHistory);
                    if (matcher.find()) {
                        int matchStart = matcher.start();
                        String prefix = allergyHistory.substring(Math.max(0, matchStart - 20), matchStart);
                        if (hasNegationPrefix(prefix)) {
                            continue;
                        }
                        return new LocalRuleResult(RULE_ID, false, "Allergy history matched for drug " + item.getDrugId(), AuditRiskLevel.BLOCK);
                    }
                }
            }
        }

        return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
    }

    private boolean hasNegationPrefix(String text) {
        String lower = text.toLowerCase();
        String[] negations = {"no ", "not ", "without ", "denies ", "no known "};
        for (String neg : negations) {
            if (lower.endsWith(neg.trim()) || lower.contains(neg)) {
                return true;
            }
        }
        return false;
    }

    private List<String> parseAllergens(String allergensJson) {
        if (allergensJson == null || allergensJson.isBlank()) {
            return List.of();
        }
        try {
            com.fasterxml.jackson.core.type.TypeReference<List<String>> typeRef = new com.fasterxml.jackson.core.type.TypeReference<>() {};
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(allergensJson, typeRef);
        } catch (Exception e) {
            return List.of();
        }
    }
}
