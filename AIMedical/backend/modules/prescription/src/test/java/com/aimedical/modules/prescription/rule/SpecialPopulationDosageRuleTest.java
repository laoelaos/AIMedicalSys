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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialPopulationDosageRuleTest {

    @Mock
    private DosageStandardRepository repository;

    private SpecialPopulationDosageRule rule;

    @BeforeEach
    void setUp() {
        rule = new SpecialPopulationDosageRule(repository);
        ReflectionTestUtils.setField(rule, "childAgeMax", 14);
        ReflectionTestUtils.setField(rule, "elderlyAgeMin", 65);
    }

    @Test
    void shouldReturnPassWhenPatientInfoNull() {
        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(new PrescriptionItem()));

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
        assertEquals(AuditRiskLevel.PASS, result.getSeverity());
    }

    @Test
    void shouldReturnPassWhenAgeNull() {
        PatientInfo patientInfo = new PatientInfo();
        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(new PrescriptionItem()));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldReturnPassForNormalAdultBetweenThresholds() {
        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(30);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(new PrescriptionItem()));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldReturnPassForAgeExactlyAtChildAgeMax() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("200"));
        standard.setAgeRangeStart(14);
        standard.setAgeRangeEnd(14);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(14);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
        verify(repository).findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral");
    }

    @Test
    void shouldReturnPassForAgeExactlyAtElderlyAgeMin() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("200"));
        standard.setAgeRangeStart(65);
        standard.setAgeRangeEnd(65);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(65);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
        verify(repository).findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral");
    }

    @Test
    void shouldCheckChildWhenAgeBelowChildAgeMax() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(2);
        standard.setAgeRangeEnd(12);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

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
        assertEquals("SPECIAL_POPULATION_DOSAGE", result.getRuleId());
    }

    @Test
    void shouldCheckElderlyWhenAgeAboveElderlyAgeMin() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("80"));
        standard.setAgeRangeStart(65);
        standard.setAgeRangeEnd(120);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(150));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(70);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldPassWhenChildDoseWithinLimit() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("100"));
        standard.setAgeRangeStart(2);
        standard.setAgeRangeEnd(12);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(80));
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
    void shouldUseSpecializedQueryNotGeneralQuery() {
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of());

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        rule.check(request);

        verify(repository).findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral");
        verify(repository, never()).findByDrugCodeAndRouteOfAdministration(anyString(), anyString());
    }

    @Test
    void shouldMatchWhenWeightRangeNullAndAgeInRange() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(2);
        standard.setAgeRangeEnd(12);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

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
    void shouldMatchWhenWeightInRangeAndAgeInRange() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(2);
        standard.setAgeRangeEnd(12);
        standard.setWeightRangeStart(new BigDecimal("10"));
        standard.setWeightRangeEnd(new BigDecimal("30"));
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

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
    void shouldSkipWhenWeightOutOfRange() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(2);
        standard.setAgeRangeEnd(12);
        standard.setWeightRangeStart(new BigDecimal("10"));
        standard.setWeightRangeEnd(new BigDecimal("30"));
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);
        patientInfo.setWeight(40.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldSkipWhenWeightNullButStandardHasWeightRange() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(2);
        standard.setAgeRangeEnd(12);
        standard.setWeightRangeStart(new BigDecimal("10"));
        standard.setWeightRangeEnd(new BigDecimal("30"));
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

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
    void shouldSkipWhenWeightRangePartiallyNull() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(2);
        standard.setAgeRangeEnd(12);
        standard.setWeightRangeStart(new BigDecimal("10"));
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);
        patientInfo.setWeight(15.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldSkipWhenAgeOutOfRange() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(2);
        standard.setAgeRangeEnd(6);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

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
    void shouldPassWhenNoStandardsFound() {
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of());

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
    void shouldUseConfigurableChildAgeMax() {
        ReflectionTestUtils.setField(rule, "childAgeMax", 18);

        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(14);
        standard.setAgeRangeEnd(18);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(16);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldUseConfigurableElderlyAgeMin() {
        ReflectionTestUtils.setField(rule, "elderlyAgeMin", 60);

        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(60);
        standard.setAgeRangeEnd(120);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(62);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldMatchWeightAtBoundaryStart() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(2);
        standard.setAgeRangeEnd(12);
        standard.setWeightRangeStart(new BigDecimal("10"));
        standard.setWeightRangeEnd(new BigDecimal("30"));
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);
        patientInfo.setWeight(10.0);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
    }

    @Test
    void shouldMatchWeightAtBoundaryEnd() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(2);
        standard.setAgeRangeEnd(12);
        standard.setWeightRangeStart(new BigDecimal("10"));
        standard.setWeightRangeEnd(new BigDecimal("30"));
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

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
    }

    @Test
    void shouldMatchAgeAtBoundaryStart() {
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(10);
        standard.setAgeRangeEnd(14);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

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
        DosageStandard standard = new DosageStandard();
        standard.setSingleMax(new BigDecimal("50"));
        standard.setAgeRangeStart(10);
        standard.setAgeRangeEnd(14);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");
        item.setDose(BigDecimal.valueOf(100));
        item.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(14);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
    }

    @Test
    void shouldCheckMultipleItemsAndBlockOnFirstExceeding() {
        DosageStandard standard1 = new DosageStandard();
        standard1.setSingleMax(new BigDecimal("100"));
        standard1.setAgeRangeStart(2);
        standard1.setAgeRangeEnd(12);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-001", "oral"))
                .thenReturn(List.of(standard1));

        DosageStandard standard2 = new DosageStandard();
        standard2.setSingleMax(new BigDecimal("200"));
        standard2.setAgeRangeStart(2);
        standard2.setAgeRangeEnd(12);
        when(repository.findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull("drug-002", "oral"))
                .thenReturn(List.of(standard2));

        PrescriptionItem item1 = new PrescriptionItem();
        item1.setDrugId("drug-001");
item1.setDose(BigDecimal.valueOf(80));
        item1.setRoute("oral");
        item1.setFrequency("tid");
        PrescriptionItem item2 = new PrescriptionItem();
        item2.setDrugId("drug-002");
        item2.setDose(BigDecimal.valueOf(300));
        item2.setRoute("oral");

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAge(10);

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item1, item2));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
        assertTrue(result.getMessage().contains("drug-002"));
    }
}
