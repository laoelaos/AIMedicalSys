package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.repository.DrugContraindicationMappingRepository;
import com.aimedical.modules.prescription.rule.entity.DrugContraindicationMapping;
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
class ContraindicationCheckRuleTest {

    @Mock
    private DrugContraindicationMappingRepository repository;

    private ContraindicationCheckRule rule;

    @BeforeEach
    void setUp() {
        rule = new ContraindicationCheckRule(repository);
    }

    @Test
    void shouldReturnPassWhenNoPatientInfo() {
        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(new PrescriptionItem()));

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldReturnPassWhenNoComorbidities() {
        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setComorbidities(null);

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldReturnBlockForAbsoluteContraindication() {
        DrugContraindicationMapping mapping = new DrugContraindicationMapping();
        mapping.setDrugCode("drug-001");
        mapping.setContraindications("[{\"diseaseName\":\"Diabetes\",\"level\":\"ABSOLUTE_CONTRAINDICATION\"}]");
        when(repository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setComorbidities(List.of("Diabetes"));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.BLOCK, result.getSeverity());
        assertEquals("CONTRAINDICATION_CHECK", result.getRuleId());
    }

    @Test
    void shouldReturnWarnForRelativeContraindication() {
        DrugContraindicationMapping mapping = new DrugContraindicationMapping();
        mapping.setDrugCode("drug-001");
        mapping.setContraindications("[{\"diseaseName\":\"Hypertension\",\"level\":\"RELATIVE_CONTRAINDICATION\"}]");
        when(repository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setComorbidities(List.of("Hypertension"));

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
    void shouldReturnPassWhenNoMatchingComorbidity() {
        DrugContraindicationMapping mapping = new DrugContraindicationMapping();
        mapping.setDrugCode("drug-001");
        mapping.setContraindications("[{\"diseaseName\":\"Diabetes\",\"level\":\"ABSOLUTE_CONTRAINDICATION\"}]");
        when(repository.findByDrugCode("drug-001")).thenReturn(Optional.of(mapping));

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setComorbidities(List.of("Asthma"));

        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));
        request.setPatientInfo(patientInfo);

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }
}
