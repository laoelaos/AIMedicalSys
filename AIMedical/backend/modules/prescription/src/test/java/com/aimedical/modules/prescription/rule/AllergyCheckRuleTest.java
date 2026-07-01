package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.patient.entity.AllergySeverity;
import com.aimedical.modules.prescription.dto.audit.AllergyDetail;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.repository.DrugAllergyMappingRepository;
import com.aimedical.modules.prescription.rule.entity.DrugAllergyMapping;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllergyCheckRuleTest {

    @Mock
    private DrugAllergyMappingRepository drugAllergyMappingRepository;

    private AllergyCheckRule rule;

    @BeforeEach
    void setUp() {
        rule = new AllergyCheckRule(drugAllergyMappingRepository);
    }

    @Test
    void shouldReturnPassWhenNoPatientInfo() {
        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(new PrescriptionItem()));

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
        assertEquals(AuditRiskLevel.PASS, result.getSeverity());
    }

    @Test
    void shouldReturnPassWhenNoAllergenMatchFound() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        mapping.setDrugCode("drug-001");
        mapping.setAllergens("[\"Penicillin\"]");
        when(drugAllergyMappingRepository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        AllergyDetail detail = new AllergyDetail();
        detail.setAllergen("Sulfa");
        detail.setSeverity(AllergySeverity.SEVERE);

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAllergyDetails(List.of(detail));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
        assertEquals(AuditRiskLevel.PASS, result.getSeverity());
    }

    @Test
    void shouldReturnBlockWhenSevereAllergyFound() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        mapping.setDrugCode("drug-001");
        mapping.setAllergens("[\"Penicillin\"]");
        when(drugAllergyMappingRepository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        AllergyDetail detail = new AllergyDetail();
        detail.setAllergen("Penicillin");
        detail.setSeverity(AllergySeverity.SEVERE);

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAllergyDetails(List.of(detail));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
        assertEquals("ALLERGY_CHECK", result.getRuleId());
    }

    @Test
    void shouldReturnWarnWhenModerateAllergyFound() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        mapping.setDrugCode("drug-001");
        mapping.setAllergens("[\"Penicillin\"]");
        when(drugAllergyMappingRepository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        AllergyDetail detail = new AllergyDetail();
        detail.setAllergen("Penicillin");
        detail.setSeverity(AllergySeverity.MODERATE);

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAllergyDetails(List.of(detail));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldReturnWarnWhenMildAllergyFound() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        mapping.setDrugCode("drug-001");
        mapping.setAllergens("[\"Penicillin\"]");
        when(drugAllergyMappingRepository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        AllergyDetail detail = new AllergyDetail();
        detail.setAllergen("Penicillin");
        detail.setSeverity(AllergySeverity.MILD);

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAllergyDetails(List.of(detail));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
    }

    @Test
    void shouldReturnBlockWhenAllergyHistoryTextMatch() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        mapping.setDrugCode("drug-001");
        mapping.setAllergens("[\"Penicillin\"]");
        when(drugAllergyMappingRepository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAllergyHistory("Penicillin allergy");

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldContinueWhenMappingNotFound() {
        when(drugAllergyMappingRepository.findByDrugCode("drug-001")).thenReturn(Optional.empty());

        AllergyDetail detail = new AllergyDetail();
        detail.setAllergen("Penicillin");
        detail.setSeverity(AllergySeverity.SEVERE);

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAllergyDetails(List.of(detail));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
        assertEquals(AuditRiskLevel.PASS, result.getSeverity());
    }

    @Test
    void shouldNotMatchPartialWord() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        mapping.setDrugCode("drug-001");
        mapping.setAllergens("[\"Penicillin\"]");
        when(drugAllergyMappingRepository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAllergyHistory("Has cillin allergy");

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldSkipWhenNegationPrefixFound() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        mapping.setDrugCode("drug-001");
        mapping.setAllergens("[\"Penicillin\"]");
        when(drugAllergyMappingRepository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAllergyHistory("No allergy to Penicillin");

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldBlockWhenAllergenFoundWithoutNegation() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        mapping.setDrugCode("drug-001");
        mapping.setAllergens("[\"Penicillin\"]");
        when(drugAllergyMappingRepository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAllergyHistory("Has Penicillin allergy");

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }

    @Test
    void shouldSkipWhenMultipleNegationPrefixes() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        mapping.setDrugCode("drug-001");
        mapping.setAllergens("[\"Penicillin\"]");
        when(drugAllergyMappingRepository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAllergyHistory("Patient denies Penicillin allergy");

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldHandleCaseInsensitiveAllergenMatch() {
        DrugAllergyMapping mapping = new DrugAllergyMapping();
        mapping.setDrugCode("drug-001");
        mapping.setAllergens("[\"penicillin\"]");
        when(drugAllergyMappingRepository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setAllergyHistory("Has Penicillin allergy");

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
    }
}
