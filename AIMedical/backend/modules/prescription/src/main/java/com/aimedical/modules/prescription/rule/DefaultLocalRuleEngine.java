package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultLocalRuleEngine implements LocalRuleEngine {

    private final AllergyCheckRule allergyCheckRule;
    private final ContraindicationCheckRule contraindicationCheckRule;
    private final DuplicateCheckRule duplicateCheckRule;
    private final DosageLimitRule dosageLimitRule;
    private final SpecialPopulationDosageRule specialPopulationDosageRule;
    private final DrugInteractionRule drugInteractionRule;

    public DefaultLocalRuleEngine(AllergyCheckRule allergyCheckRule,
                                   ContraindicationCheckRule contraindicationCheckRule,
                                   DuplicateCheckRule duplicateCheckRule,
                                   DosageLimitRule dosageLimitRule,
                                   SpecialPopulationDosageRule specialPopulationDosageRule,
                                   DrugInteractionRule drugInteractionRule) {
        this.allergyCheckRule = allergyCheckRule;
        this.contraindicationCheckRule = contraindicationCheckRule;
        this.duplicateCheckRule = duplicateCheckRule;
        this.dosageLimitRule = dosageLimitRule;
        this.specialPopulationDosageRule = specialPopulationDosageRule;
        this.drugInteractionRule = drugInteractionRule;
    }

    @Override
    public List<LocalRuleResult> check(AuditRequest request) {
        List<LocalRuleResult> results = new ArrayList<>();
        results.add(allergyCheckRule.check(request));
        results.add(contraindicationCheckRule.check(request));
        results.add(duplicateCheckRule.check(request));
        results.add(dosageLimitRule.check(request));
        results.add(specialPopulationDosageRule.check(request));
        results.add(drugInteractionRule.check(request));
        return results;
    }
}
