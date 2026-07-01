package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.repository.DrugCompositionDictRepository;
import com.aimedical.modules.prescription.rule.entity.DrugCompositionDict;
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
class DuplicateCheckRuleTest {

    @Mock
    private DrugCompositionDictRepository repository;

    private DuplicateCheckRule rule;

    @BeforeEach
    void setUp() {
        rule = new DuplicateCheckRule(repository);
    }

    @Test
    void shouldReturnPassWhenSingleItem() {
        PrescriptionItem item = new PrescriptionItem();
        item.setDrugId("drug-001");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item));

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }

    @Test
    void shouldReturnWarnWhenDuplicateIngredientFound() {
        DrugCompositionDict dict1 = new DrugCompositionDict();
        dict1.setDrugCode("drug-001");
        dict1.setIngredients("[{\"ingredientCode\":\"ING-001\",\"ingredientName\":\"Aspirin\"}]");

        DrugCompositionDict dict2 = new DrugCompositionDict();
        dict2.setDrugCode("drug-002");
        dict2.setIngredients("[{\"ingredientCode\":\"ING-001\",\"ingredientName\":\"Aspirin\"}]");

        when(repository.findByDrugCode("drug-001")).thenReturn(Optional.of(dict1));
        when(repository.findByDrugCode("drug-002")).thenReturn(Optional.of(dict2));

        PrescriptionItem item1 = new PrescriptionItem();
        item1.setDrugId("drug-001");
        PrescriptionItem item2 = new PrescriptionItem();
        item2.setDrugId("drug-002");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item1, item2));

        LocalRuleResult result = rule.check(request);

        assertFalse(result.isPassed());
        assertEquals(AuditRiskLevel.WARN, result.getSeverity());
        assertEquals("DUPLICATE_CHECK", result.getRuleId());
    }

    @Test
    void shouldReturnPassWhenNoOverlapInIngredients() {
        DrugCompositionDict dict1 = new DrugCompositionDict();
        dict1.setDrugCode("drug-001");
        dict1.setIngredients("[{\"ingredientCode\":\"ING-001\",\"ingredientName\":\"Aspirin\"}]");

        DrugCompositionDict dict2 = new DrugCompositionDict();
        dict2.setDrugCode("drug-002");
        dict2.setIngredients("[{\"ingredientCode\":\"ING-002\",\"ingredientName\":\"Paracetamol\"}]");

        when(repository.findByDrugCode("drug-001")).thenReturn(Optional.of(dict1));
        when(repository.findByDrugCode("drug-002")).thenReturn(Optional.of(dict2));

        PrescriptionItem item1 = new PrescriptionItem();
        item1.setDrugId("drug-001");
        PrescriptionItem item2 = new PrescriptionItem();
        item2.setDrugId("drug-002");

        AuditRequest request = new AuditRequest();
        request.setPrescriptionItems(List.of(item1, item2));

        LocalRuleResult result = rule.check(request);

        assertTrue(result.isPassed());
    }
}
