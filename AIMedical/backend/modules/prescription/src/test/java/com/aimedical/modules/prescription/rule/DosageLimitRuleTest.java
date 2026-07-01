package com.aimedical.modules.prescription.rule;

import com.aimedical.common.entity.DosageStandard;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.repository.DosageStandardRepository;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DosageLimitRuleTest {

    @Mock
    private DosageStandardRepository repository;

    private DosageLimitRule rule;

    @BeforeEach
    void setUp() {
        rule = new DosageLimitRule(repository);
    }

    @Test
    void shouldReturnPassWhenNoStandardsFound() {
        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of());

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(500));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldReturnPassWhenDoseWithinLimit() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("100"));
        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(80));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldReturnPassWhenDoseEqualsSingleMax() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("100"));
        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldReturnWarnWhenDoseExceedsSingleMax() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("100"));
        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(150));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
        assertEquals("DOSAGE_LIMIT", result.getRuleId());
    }

    @Test
    void shouldReturnBlockWhenDoseExceedsDoubleSingleMax() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("100"));
        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(250));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldReturnWarnWhenDoseEqualsDoubleSingleMax() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("100"));
        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(200));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldMatchLevel1ExactAgeAndWeight() {
        DosageStandard exactMatch = new DosageStandard();
        exactMatch.setSingleMax(new BigDecimal("30"));
        exactMatch.setAgeRangeStart(10);
        exactMatch.setAgeRangeEnd(10);
        exactMatch.setWeightRangeStart(new BigDecimal("30"));
        exactMatch.setWeightRangeEnd(new BigDecimal("30"));

        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("200"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(defaultStandard, exactMatch));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);
        patientInfo.setWeight(30.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldMatchLevel2AgeRangeAndWeightRange() {
        DosageStandard rangeMatch = new DosageStandard();
        rangeMatch.setSingleMax(new BigDecimal("50"));
        rangeMatch.setAgeRangeStart(2);
        rangeMatch.setAgeRangeEnd(12);
        rangeMatch.setWeightRangeStart(new BigDecimal("10"));
        rangeMatch.setWeightRangeEnd(new BigDecimal("30"));

        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("200"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(defaultStandard, rangeMatch));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);
        patientInfo.setWeight(20.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldMatchLevel3AgeRangeWeightNull() {
        DosageStandard ageOnly = new DosageStandard();
        ageOnly.setSingleMax(new BigDecimal("50"));
        ageOnly.setAgeRangeStart(2);
        ageOnly.setAgeRangeEnd(12);

        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("200"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(defaultStandard, ageOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldMatchLevel4WeightRangeAgeNull() {
        DosageStandard weightOnly = new DosageStandard();
        weightOnly.setSingleMax(new BigDecimal("50"));
        weightOnly.setWeightRangeStart(new BigDecimal("10"));
        weightOnly.setWeightRangeEnd(new BigDecimal("30"));

        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("200"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(defaultStandard, weightOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setWeight(20.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldMatchLevel5NoAgeOrWeight() {
        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("100"));

        DosageStandard specificStandard = new DosageStandard();
        specificStandard.setSingleMax(new BigDecimal("50"));
        specificStandard.setAgeRangeStart(2);
        specificStandard.setAgeRangeEnd(12);

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(defaultStandard, specificStandard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(150));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldFallbackToFirstStandardWhenNoMatch() {
        DosageStandard ageOnly = new DosageStandard();
        ageOnly.setSingleMax(new BigDecimal("40"));
        ageOnly.setAgeRangeStart(2);
        ageOnly.setAgeRangeEnd(6);

        DosageStandard weightOnly = new DosageStandard();
        weightOnly.setSingleMax(new BigDecimal("200"));
        weightOnly.setWeightRangeStart(new BigDecimal("10"));
        weightOnly.setWeightRangeEnd(new BigDecimal("20"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ageOnly, weightOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);
        patientInfo.setWeight(30.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldPreferLevel1OverLevel2() {
        DosageStandard exactMatch = new DosageStandard();
        exactMatch.setSingleMax(new BigDecimal("30"));
        exactMatch.setAgeRangeStart(10);
        exactMatch.setAgeRangeEnd(10);
        exactMatch.setWeightRangeStart(new BigDecimal("30"));
        exactMatch.setWeightRangeEnd(new BigDecimal("30"));

        DosageStandard rangeMatch = new DosageStandard();
        rangeMatch.setSingleMax(new BigDecimal("200"));
        rangeMatch.setAgeRangeStart(2);
        rangeMatch.setAgeRangeEnd(12);
        rangeMatch.setWeightRangeStart(new BigDecimal("10"));
        rangeMatch.setWeightRangeEnd(new BigDecimal("50"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(rangeMatch, exactMatch));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);
        patientInfo.setWeight(30.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldPreferLevel2OverLevel3() {
        DosageStandard rangeMatch = new DosageStandard();
        rangeMatch.setSingleMax(new BigDecimal("30"));
        rangeMatch.setAgeRangeStart(2);
        rangeMatch.setAgeRangeEnd(12);
        rangeMatch.setWeightRangeStart(new BigDecimal("10"));
        rangeMatch.setWeightRangeEnd(new BigDecimal("30"));

        DosageStandard ageOnly = new DosageStandard();
        ageOnly.setSingleMax(new BigDecimal("200"));
        ageOnly.setAgeRangeStart(2);
        ageOnly.setAgeRangeEnd(12);

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ageOnly, rangeMatch));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);
        patientInfo.setWeight(20.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldPreferLevel3OverLevel5() {
        DosageStandard ageOnly = new DosageStandard();
        ageOnly.setSingleMax(new BigDecimal("30"));
        ageOnly.setAgeRangeStart(2);
        ageOnly.setAgeRangeEnd(12);

        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("200"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(defaultStandard, ageOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldPreferLevel4OverLevel5() {
        DosageStandard weightOnly = new DosageStandard();
        weightOnly.setSingleMax(new BigDecimal("30"));
        weightOnly.setWeightRangeStart(new BigDecimal("10"));
        weightOnly.setWeightRangeEnd(new BigDecimal("30"));

        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("200"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(defaultStandard, weightOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setWeight(20.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldDemoteAgePartialToLevel5() {
        DosageStandard agePartial = new DosageStandard();
        agePartial.setSingleMax(new BigDecimal("50"));
        agePartial.setAgeRangeStart(10);

        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("200"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(agePartial, defaultStandard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldDemoteWeightPartialWithAgeCompleteToLevel3AndLevel5() {
        DosageStandard weightPartialAgeComplete = new DosageStandard();
        weightPartialAgeComplete.setSingleMax(new BigDecimal("80"));
        weightPartialAgeComplete.setAgeRangeStart(2);
        weightPartialAgeComplete.setAgeRangeEnd(12);
        weightPartialAgeComplete.setWeightRangeStart(new BigDecimal("10"));

        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("200"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(weightPartialAgeComplete, defaultStandard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldDemoteWeightPartialWithAgeNullToLevel5() {
        DosageStandard weightPartialAgeNull = new DosageStandard();
        weightPartialAgeNull.setSingleMax(new BigDecimal("50"));
        weightPartialAgeNull.setWeightRangeStart(new BigDecimal("10"));

        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("200"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(weightPartialAgeNull, defaultStandard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldDemoteBothPartialToLevel5() {
        DosageStandard bothPartial = new DosageStandard();
        bothPartial.setSingleMax(new BigDecimal("50"));
        bothPartial.setAgeRangeStart(10);
        bothPartial.setWeightRangeStart(new BigDecimal("10"));

        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("200"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(bothPartial, defaultStandard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldGetWeightFromPatientInfo() {
        DosageStandard weightOnly = new DosageStandard();
        weightOnly.setSingleMax(new BigDecimal("50"));
        weightOnly.setWeightRangeStart(new BigDecimal("10"));
        weightOnly.setWeightRangeEnd(new BigDecimal("30"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(weightOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setWeight(20.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldHandleNullPatientInfo() {
        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("100"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(defaultStandard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(150));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldHandleNullWeightWithAgeOnlyStandard() {
        DosageStandard ageOnly = new DosageStandard();
        ageOnly.setSingleMax(new BigDecimal("50"));
        ageOnly.setAgeRangeStart(2);
        ageOnly.setAgeRangeEnd(12);

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ageOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldHandleNullAgeWithWeightOnlyStandard() {
        DosageStandard weightOnly = new DosageStandard();
        weightOnly.setSingleMax(new BigDecimal("50"));
        weightOnly.setWeightRangeStart(new BigDecimal("10"));
        weightOnly.setWeightRangeEnd(new BigDecimal("30"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(weightOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setWeight(20.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldNotMatchLevel1WhenWeightNull() {
        DosageStandard exactMatch = new DosageStandard();
        exactMatch.setSingleMax(new BigDecimal("10"));
        exactMatch.setAgeRangeStart(10);
        exactMatch.setAgeRangeEnd(10);
        exactMatch.setWeightRangeStart(new BigDecimal("30"));
        exactMatch.setWeightRangeEnd(new BigDecimal("30"));

        DosageStandard ageOnly = new DosageStandard();
        ageOnly.setSingleMax(new BigDecimal("500"));
        ageOnly.setAgeRangeStart(2);
        ageOnly.setAgeRangeEnd(12);

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(exactMatch, ageOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldNotMatchLevel1WhenAgeNull() {
        DosageStandard exactMatch = new DosageStandard();
        exactMatch.setSingleMax(new BigDecimal("10"));
        exactMatch.setAgeRangeStart(10);
        exactMatch.setAgeRangeEnd(10);
        exactMatch.setWeightRangeStart(new BigDecimal("30"));
        exactMatch.setWeightRangeEnd(new BigDecimal("30"));

        DosageStandard weightOnly = new DosageStandard();
        weightOnly.setSingleMax(new BigDecimal("500"));
        weightOnly.setWeightRangeStart(new BigDecimal("10"));
        weightOnly.setWeightRangeEnd(new BigDecimal("50"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(exactMatch, weightOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setWeight(30.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldMatchWeightAtBoundaryStart() {
        DosageStandard weightOnly = new DosageStandard();
        weightOnly.setSingleMax(new BigDecimal("50"));
        weightOnly.setWeightRangeStart(new BigDecimal("10"));
        weightOnly.setWeightRangeEnd(new BigDecimal("30"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(weightOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setWeight(10.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
    }

    @Test
    void shouldMatchWeightAtBoundaryEnd() {
        DosageStandard weightOnly = new DosageStandard();
        weightOnly.setSingleMax(new BigDecimal("50"));
        weightOnly.setWeightRangeStart(new BigDecimal("10"));
        weightOnly.setWeightRangeEnd(new BigDecimal("30"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(weightOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setWeight(30.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
    }

    @Test
    void shouldMatchAgeAtBoundaryStart() {
        DosageStandard ageOnly = new DosageStandard();
        ageOnly.setSingleMax(new BigDecimal("50"));
        ageOnly.setAgeRangeStart(10);
        ageOnly.setAgeRangeEnd(20);

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ageOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
    }

    @Test
    void shouldMatchAgeAtBoundaryEnd() {
        DosageStandard ageOnly = new DosageStandard();
        ageOnly.setSingleMax(new BigDecimal("50"));
        ageOnly.setAgeRangeStart(10);
        ageOnly.setAgeRangeEnd(20);

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(ageOnly));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(20);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
    }

    @Test
    void shouldKeepFirstMatchAtEachLevel() {
        DosageStandard firstExact = new DosageStandard();
        firstExact.setSingleMax(new BigDecimal("30"));
        firstExact.setAgeRangeStart(10);
        firstExact.setAgeRangeEnd(10);
        firstExact.setWeightRangeStart(new BigDecimal("30"));
        firstExact.setWeightRangeEnd(new BigDecimal("30"));

        DosageStandard secondExact = new DosageStandard();
        secondExact.setSingleMax(new BigDecimal("200"));
        secondExact.setAgeRangeStart(10);
        secondExact.setAgeRangeEnd(10);
        secondExact.setWeightRangeStart(new BigDecimal("30"));
        secondExact.setWeightRangeEnd(new BigDecimal("30"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(firstExact, secondExact));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);
        patientInfo.setWeight(30.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldUseGeneralQueryNotSpecializedQuery() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("100"));
        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(150));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        rule.check(request);

        verify(repository).findByDrugCodeAndRouteOfAdministration("drug-001", "oral");
        verify(repository, never()).findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull(anyString(), anyString());
    }

    @Test
    void shouldHandleNullSingleMax() {
        DosageStandard standard = new DosageStandard();
        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(500));
        item.setRoute("oral");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldLogWarnWhenFindBestMatchReturnsNull() {
        DosageStandard agePartial = new DosageStandard();
        agePartial.setSingleMax(new BigDecimal("50"));
        agePartial.setAgeRangeStart(10);

        DosageStandard defaultStandard = new DosageStandard();
        defaultStandard.setSingleMax(new BigDecimal("200"));

        when(repository.findByDrugCodeAndRouteOfAdministration("drug-001", "oral"))
                .thenReturn(List.of(agePartial, defaultStandard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }
}
