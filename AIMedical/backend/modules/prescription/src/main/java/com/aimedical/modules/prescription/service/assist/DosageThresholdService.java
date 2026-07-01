package com.aimedical.modules.prescription.service.assist;

import com.aimedical.common.entity.DosageStandard;
import com.aimedical.modules.prescription.PrescriptionErrorCode;
import com.aimedical.modules.prescription.dto.assist.DosageAlert;
import com.aimedical.modules.prescription.dto.assist.DosageAlertLevel;
import com.aimedical.modules.prescription.dto.assist.DosageCheckRequest;
import com.aimedical.modules.prescription.dto.assist.DoseWarningType;
import com.aimedical.modules.prescription.repository.DosageStandardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Service
public class DosageThresholdService {

    private static final Logger log = LoggerFactory.getLogger(DosageThresholdService.class);

    private final DosageStandardRepository dosageStandardRepository;

    public DosageThresholdService(DosageStandardRepository dosageStandardRepository) {
        this.dosageStandardRepository = dosageStandardRepository;
    }

    public List<DosageAlert> check(DosageCheckRequest request) {
        List<DosageStandard> candidates = dosageStandardRepository.findByDrugCodeAndRouteOfAdministration(
                request.getDrugCode(), request.getRouteOfAdministration());

        DosageStandard matched = matchByPriority(candidates, request);
        if (matched == null) {
            DosageAlert alert = new DosageAlert();
            alert.setAlertLevel(DosageAlertLevel.CRITICAL);
            alert.setWarningType(DoseWarningType.OVER_SINGLE_DOSE);
            alert.setDrugCode(request.getDrugCode());
            alert.setCurrentDose(request.getDosage());
            alert.setMessage("剂量标准未找到");
            alert.setErrorCode(PrescriptionErrorCode.RX_ASSIST_DOSE_STANDARD_NOT_FOUND.getCode());
            return List.of(alert);
        }

        if (!matched.getUnit().equalsIgnoreCase(request.getUnit())) {
            DosageAlert alert = new DosageAlert();
            alert.setAlertLevel(DosageAlertLevel.WARNING);
            alert.setWarningType(DoseWarningType.OVER_SINGLE_DOSE);
            alert.setDrugCode(request.getDrugCode());
            alert.setCurrentDose(request.getDosage());
            alert.setMessage("单位不匹配：请求单位 " + request.getUnit() + "，标准单位 " + matched.getUnit());
            return List.of(alert);
        }

        List<DosageAlert> alerts = new ArrayList<>();

        BigDecimal dosage = BigDecimal.valueOf(request.getDosage());

        if (dosage.compareTo(matched.getSingleMax().multiply(BigDecimal.valueOf(2))) > 0) {
            DosageAlert alert = new DosageAlert();
            alert.setAlertLevel(DosageAlertLevel.CRITICAL);
            alert.setWarningType(DoseWarningType.OVER_SINGLE_DOSE);
            alert.setDrugCode(request.getDrugCode());
            alert.setCurrentDose(request.getDosage());
            alert.setSuggestedValue(matched.getSingleMax());
            alert.setMessage("单次剂量超过上限2倍");
            alerts.add(alert);
        } else if (dosage.compareTo(matched.getSingleMax()) > 0) {
            DosageAlert alert = new DosageAlert();
            alert.setAlertLevel(DosageAlertLevel.WARNING);
            alert.setWarningType(DoseWarningType.OVER_SINGLE_DOSE);
            alert.setDrugCode(request.getDrugCode());
            alert.setCurrentDose(request.getDosage());
            alert.setSuggestedValue(matched.getSingleMax());
            alert.setMessage("单次剂量超过上限");
            alerts.add(alert);
        }

        if (request.getFrequency() != null && matched.getDailyMax() != null) {
            try {
                int frequencyPerDay = Integer.parseInt(request.getFrequency());
                BigDecimal dailyDose = dosage.multiply(BigDecimal.valueOf(frequencyPerDay));
                if (dailyDose.compareTo(matched.getDailyMax()) > 0) {
                    DosageAlert alert = new DosageAlert();
                    alert.setAlertLevel(DosageAlertLevel.WARNING);
                    alert.setWarningType(DoseWarningType.OVER_DAILY_DOSE);
                    alert.setDrugCode(request.getDrugCode());
                    alert.setCurrentDose(request.getDosage());
                    alert.setSuggestedValue(matched.getDailyMax());
                    alert.setMessage("日剂量超过上限");
                    alerts.add(alert);
                }
            } catch (NumberFormatException e) {
                log.warn("Non-numeric frequency: {}, skipping daily dose check", request.getFrequency());
            }
        }

        return alerts;
    }

    private DosageStandard matchByPriority(List<DosageStandard> candidates, DosageCheckRequest request) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Integer age = request.getPatientAge();
        BigDecimal weight = request.getPatientWeight();

        DosageStandard result;

        result = findFirstCandidate(candidates, ds ->
                matchesBothRanges(ds, age, weight)
                && ds.getAgeRangeStart() != null && ds.getAgeRangeEnd() != null
                && ds.getWeightRangeStart() != null && ds.getWeightRangeEnd() != null);
        if (result != null) return result;

        result = findFirstCandidate(candidates, ds -> matchesBothRanges(ds, age, weight));
        if (result != null) return result;

        result = findFirstCandidate(candidates, ds ->
                age != null
                && isInRange(age, ds.getAgeRangeStart(), ds.getAgeRangeEnd())
                && ds.getWeightRangeStart() == null && ds.getWeightRangeEnd() == null);
        if (result != null) return result;

        result = findFirstCandidate(candidates, ds ->
                weight != null
                && isInRange(weight, ds.getWeightRangeStart(), ds.getWeightRangeEnd())
                && ds.getAgeRangeStart() == null && ds.getAgeRangeEnd() == null);
        if (result != null) return result;

        result = findFirstCandidate(candidates, ds ->
                ds.getAgeRangeStart() == null && ds.getAgeRangeEnd() == null
                && ds.getWeightRangeStart() == null && ds.getWeightRangeEnd() == null);
        if (result != null) return result;

        return null;
    }

    private boolean matchesBothRanges(DosageStandard ds, Integer age, BigDecimal weight) {
        return age != null && weight != null
                && isInRange(age, ds.getAgeRangeStart(), ds.getAgeRangeEnd())
                && isInRange(weight, ds.getWeightRangeStart(), ds.getWeightRangeEnd());
    }

    private DosageStandard findFirstCandidate(List<DosageStandard> candidates, Predicate<DosageStandard> predicate) {
        for (DosageStandard ds : candidates) {
            if (predicate.test(ds)) {
                return ds;
            }
        }
        return null;
    }

    private boolean isInRange(Integer value, Integer rangeStart, Integer rangeEnd) {
        if (rangeStart == null && rangeEnd == null) return true;
        if (rangeStart != null && rangeEnd != null) {
            return value >= rangeStart && value <= rangeEnd;
        }
        return true;
    }

    private boolean isInRange(BigDecimal value, BigDecimal rangeStart, BigDecimal rangeEnd) {
        if (rangeStart == null && rangeEnd == null) return true;
        if (rangeStart != null && rangeEnd != null) {
            return value.compareTo(rangeStart) >= 0 && value.compareTo(rangeEnd) <= 0;
        }
        return true;
    }
}
