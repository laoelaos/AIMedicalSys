package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "prescription.rule.drug-interaction.enabled", havingValue = "true", matchIfMissing = true)
public class DrugInteractionRule {

    private static final String RULE_ID = "DRUG_INTERACTION";

    public LocalRuleResult check(AuditRequest request) {
        return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
    }
}
