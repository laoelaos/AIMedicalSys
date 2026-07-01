package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultLocalRuleEngineTest {

    @Mock private AllergyCheckRule allergyCheckRule;
    @Mock private ContraindicationCheckRule contraindicationCheckRule;
    @Mock private DuplicateCheckRule duplicateCheckRule;
    @Mock private DosageLimitRule dosageLimitRule;
    @Mock private SpecialPopulationDosageRule specialPopulationDosageRule;
    @Mock private DrugInteractionRule drugInteractionRule;

    @InjectMocks
    private DefaultLocalRuleEngine engine;

    @Test
    void shouldExecuteAllSixRules() {
        AuditRequest request = new AuditRequest();

        when(allergyCheckRule.check(request)).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(contraindicationCheckRule.check(request)).thenReturn(new LocalRuleResult("CONTRAINDICATION_CHECK", true, null, AuditRiskLevel.PASS));
        when(duplicateCheckRule.check(request)).thenReturn(new LocalRuleResult("DUPLICATE_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageLimitRule.check(request)).thenReturn(new LocalRuleResult("DOSAGE_LIMIT", true, null, AuditRiskLevel.PASS));
        when(specialPopulationDosageRule.check(request)).thenReturn(new LocalRuleResult("SPECIAL_POPULATION_DOSAGE", true, null, AuditRiskLevel.PASS));
        when(drugInteractionRule.check(request)).thenReturn(new LocalRuleResult("DRUG_INTERACTION", true, null, AuditRiskLevel.PASS));

        List<LocalRuleResult> results = engine.check(request);

        assertEquals(6, results.size());
        verify(allergyCheckRule).check(request);
        verify(contraindicationCheckRule).check(request);
        verify(duplicateCheckRule).check(request);
        verify(dosageLimitRule).check(request);
        verify(specialPopulationDosageRule).check(request);
        verify(drugInteractionRule).check(request);
    }
}
