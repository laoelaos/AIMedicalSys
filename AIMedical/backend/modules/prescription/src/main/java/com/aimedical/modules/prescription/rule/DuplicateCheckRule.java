package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.repository.DrugCompositionDictRepository;
import com.aimedical.modules.prescription.rule.entity.DrugCompositionDict;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class DuplicateCheckRule {

    private static final String RULE_ID = "DUPLICATE_CHECK";

    private final DrugCompositionDictRepository drugCompositionDictRepository;

    public DuplicateCheckRule(DrugCompositionDictRepository drugCompositionDictRepository) {
        this.drugCompositionDictRepository = drugCompositionDictRepository;
    }

    public LocalRuleResult check(AuditRequest request) {
        Map<String, Set<String>> drugIngredients = new HashMap<>();

        for (PrescriptionItem item : request.getPrescriptionItems()) {
            Optional<DrugCompositionDict> dictOpt = drugCompositionDictRepository.findByDrugCode(item.getDrugId());
            if (dictOpt.isPresent()) {
                Set<String> ingredients = parseIngredients(dictOpt.get().getIngredients());
                drugIngredients.put(item.getDrugId(), ingredients);
            }
        }

        List<String> drugCodes = List.copyOf(drugIngredients.keySet());
        for (int i = 0; i < drugCodes.size(); i++) {
            for (int j = i + 1; j < drugCodes.size(); j++) {
                Set<String> intersection = new HashSet<>(drugIngredients.get(drugCodes.get(i)));
                intersection.retainAll(drugIngredients.get(drugCodes.get(j)));
                if (!intersection.isEmpty()) {
                    return new LocalRuleResult(RULE_ID, false,
                            "Duplicate ingredients detected between drugs " + drugCodes.get(i) + " and " + drugCodes.get(j) + ": " + intersection,
                            AuditRiskLevel.WARN);
                }
            }
        }

        return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
    }

    private Set<String> parseIngredients(String ingredientsJson) {
        if (ingredientsJson == null || ingredientsJson.isBlank()) {
            return Set.of();
        }
        try {
            com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>> typeRef = new com.fasterxml.jackson.core.type.TypeReference<>() {};
            List<Map<String, String>> items = new com.fasterxml.jackson.databind.ObjectMapper().readValue(ingredientsJson, typeRef);
            Set<String> codes = new HashSet<>();
            for (Map<String, String> item : items) {
                String code = item.get("ingredientCode");
                if (code != null) {
                    codes.add(code);
                }
            }
            return codes;
        } catch (Exception e) {
            return Set.of();
        }
    }
}
