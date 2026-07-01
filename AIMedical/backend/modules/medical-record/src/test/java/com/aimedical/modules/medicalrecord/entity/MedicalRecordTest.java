package com.aimedical.modules.medicalrecord.entity;

import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordTest {

    @Test
    void shouldCreateWithDefaultValues() {
        MedicalRecord entity = new MedicalRecord();
        assertNull(entity.getRecordId());
        assertNull(entity.getPatientId());
        assertNull(entity.getVisitId());
        assertNull(entity.getDepartmentId());
        assertNull(entity.getContent());
        assertNull(entity.getDoctorId());
        assertNull(entity.getVisitIdFallback());
        assertNull(entity.getVersion());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void shouldSetAndGetAllFields() {
        MedicalRecord entity = new MedicalRecord();
        entity.setRecordId(1L);
        entity.setPatientId("P001");
        entity.setVisitId("V001");
        entity.setDepartmentId("dept-01");
        Map<MedicalRecordField, String> content = Map.of(MedicalRecordField.CHIEF_COMPLAINT, "头痛");
        entity.setContent(content);
        entity.setDoctorId("D001");
        entity.setVisitIdFallback(true);
        entity.setVersion(1);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertEquals(1L, entity.getRecordId());
        assertEquals("P001", entity.getPatientId());
        assertEquals("V001", entity.getVisitId());
        assertEquals("dept-01", entity.getDepartmentId());
        assertSame(content, entity.getContent());
        assertEquals("D001", entity.getDoctorId());
        assertTrue(entity.getVisitIdFallback());
        assertEquals(1, entity.getVersion());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void prePersistShouldSetCreatedAt() {
        MedicalRecord entity = new MedicalRecord();
        entity.prePersist();
        assertNotNull(entity.getCreatedAt());
        assertTrue(entity.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void prePersistShouldSetUpdatedAt() {
        MedicalRecord entity = new MedicalRecord();
        entity.prePersist();
        assertNotNull(entity.getUpdatedAt());
        assertTrue(entity.getUpdatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void preUpdateShouldSetUpdatedAt() {
        MedicalRecord entity = new MedicalRecord();
        entity.preUpdate();
        assertNotNull(entity.getUpdatedAt());
        assertTrue(entity.getUpdatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }
}
