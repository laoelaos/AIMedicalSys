package com.aimedical.modules.medicalrecord.detector;

import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse;
import com.aimedical.modules.medicalrecord.converter.MedicalRecordConverter;
import com.aimedical.modules.medicalrecord.dto.FieldMissingHint;
import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import com.aimedical.modules.medicalrecord.template.DepartmentTemplateConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MissingFieldDetectorImplTest {

    private final MedicalRecordConverter converter = new MedicalRecordConverter(new ObjectMapper());
    private final MissingFieldDetectorImpl detector = new MissingFieldDetectorImpl(converter);
    private DepartmentTemplateConfig template;

    @BeforeEach
    void setUp() {
        Set<MedicalRecordField> allFields = Stream.of(MedicalRecordField.values())
                .filter(f -> f != MedicalRecordField.MISSING_FIELDS && f != MedicalRecordField.PARTIAL_CONTENT)
                .collect(Collectors.toSet());
        Map<MedicalRecordField, String> prompts = new HashMap<>();
        Map<MedicalRecordField, String> actions = new HashMap<>();
        for (MedicalRecordField f : allFields) {
            prompts.put(f, "{{fieldName}}字段缺失");
            actions.put(f, "请补充{{fieldName}}信息");
        }
        template = new DepartmentTemplateConfig("dept-01", allFields, prompts, actions);
    }

    @Test
    void shouldReturnEmptyHintsWhenAllFieldsAreFilled() {
        MedicalRecordGenResponse resp = fullResponse();
        List<FieldMissingHint> hints = detector.detect(resp, template);
        assertTrue(hints.isEmpty());
    }

    @Test
    void shouldReturnHintForNullField() {
        MedicalRecordGenResponse resp = fullResponse();
        resp.setChiefComplaint(null);
        List<FieldMissingHint> hints = detector.detect(resp, template);
        assertEquals(1, hints.size());
        assertEquals(MedicalRecordField.CHIEF_COMPLAINT, hints.get(0).getMissingField());
    }

    @Test
    void shouldReturnHintForEmptyStringField() {
        MedicalRecordGenResponse resp = fullResponse();
        resp.setChiefComplaint("");
        List<FieldMissingHint> hints = detector.detect(resp, template);
        assertEquals(1, hints.size());
        assertEquals(MedicalRecordField.CHIEF_COMPLAINT, hints.get(0).getMissingField());
    }

    @Test
    void shouldReturnHintForBlankStringField() {
        MedicalRecordGenResponse resp = fullResponse();
        resp.setChiefComplaint("   ");
        List<FieldMissingHint> hints = detector.detect(resp, template);
        assertEquals(1, hints.size());
        assertEquals(MedicalRecordField.CHIEF_COMPLAINT, hints.get(0).getMissingField());
    }

    @Test
    void shouldDetectMultipleMissingFields() {
        MedicalRecordGenResponse resp = fullResponse();
        resp.setChiefComplaint(null);
        resp.setSymptomDescription(null);
        resp.setPresentIllness(null);
        resp.setMissingFields(null);
        resp.setPartialContent(null);
        List<FieldMissingHint> hints = detector.detect(resp, template);
        assertEquals(3, hints.size());
    }

    @Test
    void shouldReturnHintsForAllFieldsWhenAllNull() {
        MedicalRecordGenResponse resp = new MedicalRecordGenResponse();
        List<FieldMissingHint> hints = detector.detect(resp, template);
        assertEquals(7, hints.size());
    }

    @Test
    void shouldResolvePlaceholderInPromptMessage() {
        MedicalRecordGenResponse resp = fullResponse();
        resp.setChiefComplaint(null);
        List<FieldMissingHint> hints = detector.detect(resp, template);
        assertEquals("主诉字段缺失", hints.get(0).getPromptMessage());
        assertEquals("请补充主诉信息", hints.get(0).getSuggestedAction());
    }

    @Test
    void shouldUseCustomPromptFromTemplate() {
        Map<MedicalRecordField, String> prompts = Map.of(MedicalRecordField.CHIEF_COMPLAINT, "请填写{{fieldName}}");
        Map<MedicalRecordField, String> actions = Map.of(MedicalRecordField.CHIEF_COMPLAINT, "请补充{{fieldName}}");
        Set<MedicalRecordField> fields = Set.of(MedicalRecordField.CHIEF_COMPLAINT);
        DepartmentTemplateConfig customTemplate = new DepartmentTemplateConfig("custom", fields, prompts, actions);

        MedicalRecordGenResponse resp = new MedicalRecordGenResponse();
        List<FieldMissingHint> hints = detector.detect(resp, customTemplate);

        assertEquals("请填写主诉", hints.get(0).getPromptMessage());
        assertEquals("请补充主诉", hints.get(0).getSuggestedAction());
    }

    @Test
    void shouldUseDefaultTextWhenTemplateHasNoConfigForField() {
        Map<MedicalRecordField, String> prompts = Collections.emptyMap();
        Map<MedicalRecordField, String> actions = Collections.emptyMap();
        Set<MedicalRecordField> fields = Set.of(MedicalRecordField.CHIEF_COMPLAINT);
        DepartmentTemplateConfig customTemplate = new DepartmentTemplateConfig("custom", fields, prompts, actions);

        MedicalRecordGenResponse resp = new MedicalRecordGenResponse();
        List<FieldMissingHint> hints = detector.detect(resp, customTemplate);

        assertEquals("主诉字段缺失", hints.get(0).getPromptMessage());
        assertEquals("请补充主诉信息", hints.get(0).getSuggestedAction());
    }

    @Test
    void shouldResolveAllPlaceholdersForAllFields() {
        MedicalRecordGenResponse resp = new MedicalRecordGenResponse();
        resp.setChiefComplaint(null);
        resp.setSymptomDescription(null);
        resp.setPresentIllness(null);
        resp.setPastHistory(null);
        resp.setPhysicalExam(null);
        resp.setPreliminaryDiagnosis(null);
        resp.setTreatmentPlan(null);

        List<FieldMissingHint> hints = detector.detect(resp, template);
        assertEquals(7, hints.size());

        Map<MedicalRecordField, String> expectedPrompts = new HashMap<>();
        expectedPrompts.put(MedicalRecordField.CHIEF_COMPLAINT, "主诉字段缺失");
        expectedPrompts.put(MedicalRecordField.SYMPTOM_DESCRIPTION, "症状描述字段缺失");
        expectedPrompts.put(MedicalRecordField.PRESENT_ILLNESS, "现病史字段缺失");
        expectedPrompts.put(MedicalRecordField.PAST_HISTORY, "既往史字段缺失");
        expectedPrompts.put(MedicalRecordField.PHYSICAL_EXAM, "体格检查字段缺失");
        expectedPrompts.put(MedicalRecordField.PRELIMINARY_DIAGNOSIS, "初步诊断字段缺失");
        expectedPrompts.put(MedicalRecordField.TREATMENT_PLAN, "治疗方案字段缺失");

        for (FieldMissingHint hint : hints) {
            assertEquals(expectedPrompts.get(hint.getMissingField()), hint.getPromptMessage());
        }
    }

    private static MedicalRecordGenResponse fullResponse() {
        MedicalRecordGenResponse resp = new MedicalRecordGenResponse();
        resp.setChiefComplaint("头痛");
        resp.setSymptomDescription("跳痛");
        resp.setPresentIllness("3天");
        resp.setPastHistory("无");
        resp.setPhysicalExam("正常");
        resp.setPreliminaryDiagnosis("偏头痛");
        resp.setTreatmentPlan("休息");
        resp.setMissingFields(List.of("none"));
        resp.setPartialContent(Collections.emptyMap());
        return resp;
    }
}
