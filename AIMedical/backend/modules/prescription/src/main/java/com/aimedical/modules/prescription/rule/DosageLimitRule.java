package com.aimedical.modules.prescription.rule;

import com.aimedical.common.entity.DosageStandard;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.repository.DosageStandardRepository;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class DosageLimitRule {

    private static final Logger log = LoggerFactory.getLogger(DosageLimitRule.class);
    private static final String RULE_ID = "DOSAGE_LIMIT";

    private final DosageStandardRepository dosageStandardRepository;

    public DosageLimitRule(DosageStandardRepository dosageStandardRepository) {
        this.dosageStandardRepository = dosageStandardRepository;
    }

    public LocalRuleResult check(AuditRequest request) {
        PatientInfo patientInfo = request.getPatientInfo();
        Integer patientAge = (patientInfo != null) ? patientInfo.getAge() : null;
        Double patientWeight = (patientInfo != null) ? patientInfo.getWeight() : null;

        for (PrescriptionItem item : request.getPrescriptionItems()) {
            List<DosageStandard> standards = dosageStandardRepository.findByDrugCodeAndRouteOfAdministration(item.getDrugId(), item.getRoute());
            if (standards.isEmpty()) {
                continue;
            }

            DosageStandard matched = findBestMatch(standards, patientAge, patientWeight);
            if (matched == null) {
                log.warn("findBestMatch returned null for drug {}, route {}, age={}, weight={}; falling back to first standard",
                        item.getDrugId(), item.getRoute(), patientAge, patientWeight);
                matched = standards.get(0);
            }

            BigDecimal dose = item.getDose();
            BigDecimal singleMax = matched.getSingleMax();

            if (singleMax != null && dose.compareTo(singleMax.multiply(BigDecimal.valueOf(2))) > 0) {
                return new LocalRuleResult(RULE_ID, false, "Dose " + dose + " exceeds double the max limit " + singleMax + " for drug " + item.getDrugId(), AuditRiskLevel.BLOCK);
            }
            if (singleMax != null && dose.compareTo(singleMax) > 0) {
                return new LocalRuleResult(RULE_ID, false, "Dose " + dose + " exceeds max limit " + singleMax + " for drug " + item.getDrugId(), AuditRiskLevel.WARN);
            }
        }

        return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
    }

    private DosageStandard findBestMatch(List<DosageStandard> standards, Integer age, Double weight) {
        DosageStandard level1 = null;
        DosageStandard level2 = null;
        DosageStandard level3 = null;
        DosageStandard level4 = null;
        DosageStandard level5 = null;

        for (DosageStandard ds : standards) {
            Integer as = ds.getAgeRangeStart();
            Integer ae = ds.getAgeRangeEnd();
            BigDecimal ws = ds.getWeightRangeStart();
            BigDecimal we = ds.getWeightRangeEnd();

            boolean ageNull = (as == null && ae == null);
            boolean ageComplete = (as != null && ae != null);
            boolean agePartial = (as != null) != (ae != null);
            boolean weightNull = (ws == null && we == null);
            boolean weightComplete = (ws != null && we != null);
            boolean weightPartial = (ws != null) != (we != null);

            if (agePartial || weightPartial) {
                if (agePartial && !weightPartial) {
                    if (level5 == null) {
                        level5 = ds;
                    }
                } else if (weightPartial && !agePartial) {
                    if (ageComplete && age != null
                            && age >= as && age <= ae && level3 == null) {
                        level3 = ds;
                    }
                    if (ageNull && level5 == null) {
                        level5 = ds;
                    }
                } else {
                    if (level5 == null) {
                        level5 = ds;
                    }
                }
                continue;
            }

            boolean ageInRange = ageComplete && age != null && age >= as && age <= ae;
            boolean ageExact = ageComplete && age != null && as.equals(ae) && as.equals(age);
            boolean weightInRange = weightComplete && weight != null
                    && BigDecimal.valueOf(weight).compareTo(ws) >= 0
                    && BigDecimal.valueOf(weight).compareTo(we) <= 0;
            boolean weightExact = weightComplete && weight != null
                    && ws.compareTo(we) == 0
                    && ws.compareTo(BigDecimal.valueOf(weight)) == 0;

            if (ageExact && weightExact && level1 == null) {
                level1 = ds;
            }
            if (ageInRange && weightInRange && level2 == null) {
                level2 = ds;
            }
            if (ageInRange && weightNull && level3 == null) {
                level3 = ds;
            }
            if (ageNull && weightInRange && level4 == null) {
                level4 = ds;
            }
            if (ageNull && weightNull && level5 == null) {
                level5 = ds;
            }
        }

        if (level1 != null) return level1;
        if (level2 != null) return level2;
        if (level3 != null) return level3;
        if (level4 != null) return level4;
        if (level5 != null) return level5;
        return null;
    }
}
