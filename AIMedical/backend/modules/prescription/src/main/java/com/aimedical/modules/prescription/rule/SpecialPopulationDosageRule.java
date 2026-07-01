package com.aimedical.modules.prescription.rule;

import com.aimedical.common.entity.DosageStandard;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.repository.DosageStandardRepository;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class SpecialPopulationDosageRule {

    private static final String RULE_ID = "SPECIAL_POPULATION_DOSAGE";

    private final DosageStandardRepository dosageStandardRepository;

    @Value("${special-population.child-age-max:14}")
    private int childAgeMax;

    @Value("${special-population.elderly-age-min:65}")
    private int elderlyAgeMin;

    public SpecialPopulationDosageRule(DosageStandardRepository dosageStandardRepository) {
        this.dosageStandardRepository = dosageStandardRepository;
    }

    public LocalRuleResult check(AuditRequest request) {
        PatientInfo patientInfo = request.getPatientInfo();
        if (patientInfo == null || patientInfo.getAge() == null) {
            return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
        }

        int age = patientInfo.getAge();
        if (age > childAgeMax && age < elderlyAgeMin) {
            return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
        }

        Double patientWeight = patientInfo.getWeight();

        for (PrescriptionItem item : request.getPrescriptionItems()) {
            List<DosageStandard> standards = dosageStandardRepository
                .findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull(
                    item.getDrugId(), item.getRoute());
            if (standards.isEmpty()) {
                continue;
            }

            for (DosageStandard ds : standards) {
                boolean ageMatch = age >= ds.getAgeRangeStart() && age <= ds.getAgeRangeEnd();
                boolean weightMatch = (ds.getWeightRangeStart() == null && ds.getWeightRangeEnd() == null)
                    || (patientWeight != null
                        && ds.getWeightRangeStart() != null && ds.getWeightRangeEnd() != null
                        && BigDecimal.valueOf(patientWeight).compareTo(ds.getWeightRangeStart()) >= 0
                        && BigDecimal.valueOf(patientWeight).compareTo(ds.getWeightRangeEnd()) <= 0);

                if (ageMatch && weightMatch) {
                    BigDecimal dose = item.getDose();
                    BigDecimal singleMax = ds.getSingleMax();
                    if (singleMax != null && dose.compareTo(singleMax) > 0) {
                        return new LocalRuleResult(RULE_ID, false,
                            "Special population dose " + dose + " exceeds limit " + singleMax
                                + " for drug " + item.getDrugId(),
                            AuditRiskLevel.BLOCK);
                    }
                }
            }
        }

        return new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS);
    }
}
