package com.aimedical.modules.ai.api.dto.prescription;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionDtoTest {

    // ===== PrescriptionCheckResponse =====

    @Test
    void shouldCreatePrescriptionCheckResponseWithDefaultConstructor() {
        PrescriptionCheckResponse response = new PrescriptionCheckResponse();
        assertNull(response.getRiskLevel());
        assertNull(response.getWarnings());
        assertFalse(response.isPassed());
    }

    @Test
    void shouldSetAndGetRiskLevel() {
        PrescriptionCheckResponse response = new PrescriptionCheckResponse();
        response.setRiskLevel("高");
        assertEquals("高", response.getRiskLevel());
    }

    @Test
    void shouldSetAndGetWarnings() {
        PrescriptionCheckResponse response = new PrescriptionCheckResponse();
        List<String> warnings = new ArrayList<>();
        warnings.add("与现有药物存在相互作用");
        warnings.add("剂量超出常规范围");
        response.setWarnings(warnings);
        assertEquals(2, response.getWarnings().size());
        assertEquals("与现有药物存在相互作用", response.getWarnings().get(0));
    }

    @Test
    void shouldSetAndIsPassed() {
        PrescriptionCheckResponse response = new PrescriptionCheckResponse();
        response.setPassed(true);
        assertTrue(response.isPassed());

        response.setPassed(false);
        assertFalse(response.isPassed());
    }

    @Test
    void shouldBuildFullPrescriptionCheckResponse() {
        List<String> warnings = new ArrayList<>();
        warnings.add("过敏史提示");

        PrescriptionCheckResponse response = new PrescriptionCheckResponse();
        response.setRiskLevel("中");
        response.setWarnings(warnings);
        response.setPassed(false);

        assertEquals("中", response.getRiskLevel());
        assertEquals(1, response.getWarnings().size());
        assertFalse(response.isPassed());
    }

    // ===== PrescriptionAssistResponse =====

    @Test
    void shouldCreatePrescriptionAssistResponseWithDefaultConstructor() {
        PrescriptionAssistResponse response = new PrescriptionAssistResponse();
        assertNull(response.getDrugs());
        assertNull(response.getSummary());
    }

    @Test
    void shouldSetAndGetDrugs() {
        PrescriptionAssistResponse response = new PrescriptionAssistResponse();
        List<PrescriptionAssistResponse.RecommendedDrug> drugs = new ArrayList<>();
        drugs.add(new PrescriptionAssistResponse.RecommendedDrug());
        response.setDrugs(drugs);
        assertEquals(1, response.getDrugs().size());
    }

    @Test
    void shouldSetAndGetSummary() {
        PrescriptionAssistResponse response = new PrescriptionAssistResponse();
        response.setSummary("建议联合用药方案");
        assertEquals("建议联合用药方案", response.getSummary());
    }

    @Test
    void shouldCreateRecommendedDrugWithDefaultConstructor() {
        PrescriptionAssistResponse.RecommendedDrug drug = new PrescriptionAssistResponse.RecommendedDrug();
        assertNull(drug.getDrugName());
        assertNull(drug.getSpecification());
        assertNull(drug.getDosage());
        assertNull(drug.getFrequency());
        assertNull(drug.getReason());
    }

    @Test
    void shouldCreateRecommendedDrugWithAllArgsConstructor() {
        PrescriptionAssistResponse.RecommendedDrug drug =
                new PrescriptionAssistResponse.RecommendedDrug(
                        "阿莫西林", "0.25g", "每次1粒", "每日三次", "抗感染治疗");

        assertEquals("阿莫西林", drug.getDrugName());
        assertEquals("0.25g", drug.getSpecification());
        assertEquals("每次1粒", drug.getDosage());
        assertEquals("每日三次", drug.getFrequency());
        assertEquals("抗感染治疗", drug.getReason());
    }

    @Test
    void shouldSetAndGetAllRecommendedDrugFields() {
        PrescriptionAssistResponse.RecommendedDrug drug = new PrescriptionAssistResponse.RecommendedDrug();
        drug.setDrugName("布洛芬");
        drug.setSpecification("0.2g");
        drug.setDosage("每次1片");
        drug.setFrequency("每日两次");
        drug.setReason("缓解疼痛");

        assertEquals("布洛芬", drug.getDrugName());
        assertEquals("0.2g", drug.getSpecification());
        assertEquals("每次1片", drug.getDosage());
        assertEquals("每日两次", drug.getFrequency());
        assertEquals("缓解疼痛", drug.getReason());
    }

    @Test
    void shouldBuildFullPrescriptionAssistResponse() {
        PrescriptionAssistResponse.RecommendedDrug drug1 =
                new PrescriptionAssistResponse.RecommendedDrug(
                        "阿莫西林", "0.25g", "每次1粒", "每日三次", "抗感染");
        PrescriptionAssistResponse.RecommendedDrug drug2 =
                new PrescriptionAssistResponse.RecommendedDrug();
        drug2.setDrugName("布洛芬");
        drug2.setSpecification("0.2g");
        drug2.setDosage("每次1片");
        drug2.setFrequency("每日两次");
        drug2.setReason("退热");

        List<PrescriptionAssistResponse.RecommendedDrug> drugs = new ArrayList<>();
        drugs.add(drug1);
        drugs.add(drug2);

        PrescriptionAssistResponse response = new PrescriptionAssistResponse();
        response.setDrugs(drugs);
        response.setSummary("抗感染+退热联合方案");

        assertEquals(2, response.getDrugs().size());
        assertEquals("阿莫西林", response.getDrugs().get(0).getDrugName());
        assertEquals("布洛芬", response.getDrugs().get(1).getDrugName());
        assertEquals("抗感染+退热联合方案", response.getSummary());
    }
}
