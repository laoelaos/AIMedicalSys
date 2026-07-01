package com.aimedical.modules.medicalrecord.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecordGenerateRequestTest {

    private static Validator validator;
    private static ValidatorFactory factory;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void shouldCreateWithDefaultValues() {
        RecordGenerateRequest req = new RecordGenerateRequest();
        assertNull(req.getDialogueText());
        assertNull(req.getPatientId());
        assertNull(req.getEncounterId());
        assertFalse(req.isStream());
        assertNull(req.getDepartmentId());
        assertNull(req.getDoctorId());
    }

    @Test
    void shouldSetAndGetAllFields() {
        RecordGenerateRequest req = new RecordGenerateRequest();
        req.setDialogueText("医生：你好\n患者：我头痛，已经持续了三天，每次发作时感觉头部跳痛，伴有恶心。");
        req.setPatientId("P001");
        req.setEncounterId("E001");
        req.setStream(true);
        req.setDepartmentId("dept-01");
        req.setDoctorId("D001");

        assertEquals("医生：你好\n患者：我头痛，已经持续了三天，每次发作时感觉头部跳痛，伴有恶心。", req.getDialogueText());
        assertEquals("P001", req.getPatientId());
        assertEquals("E001", req.getEncounterId());
        assertTrue(req.isStream());
        assertEquals("dept-01", req.getDepartmentId());
        assertEquals("D001", req.getDoctorId());
    }

    @Test
    void shouldFailValidationWhenDialogueTextIsNull() {
        RecordGenerateRequest req = new RecordGenerateRequest();
        req.setPatientId("P001");
        req.setEncounterId("E001");
        req.setDepartmentId("dept-01");

        var violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dialogueText")));
    }

    @Test
    void shouldFailValidationWhenDialogueTextIsTooShort() {
        RecordGenerateRequest req = new RecordGenerateRequest();
        req.setDialogueText("简短");
        req.setPatientId("P001");

        var violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dialogueText")));
    }

    @Test
    void shouldFailValidationWhenDialogueTextIsTooLong() {
        RecordGenerateRequest req = new RecordGenerateRequest();
        req.setDialogueText("A".repeat(10001));
        req.setPatientId("P001");

        var violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dialogueText")));
    }

    @Test
    void shouldPassValidationWithValidDialogueText() {
        RecordGenerateRequest req = new RecordGenerateRequest();
        req.setDialogueText("A".repeat(50));
        req.setPatientId("P001");
        req.setEncounterId("E001");
        req.setDepartmentId("dept-01");

        var violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }
}
