package com.aimedical.modules.medicalrecord.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class DeptTemplateConfigTest {

    @Test
    void shouldCreateWithDefaultValues() {
        DeptTemplateConfig entity = new DeptTemplateConfig();
        assertNull(entity.getId());
        assertNull(entity.getDepartmentId());
        assertNull(entity.getRequiredFields());
        assertNull(entity.getTemplateFields());
        assertNull(entity.getVersion());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void shouldSetAndGetAllFields() {
        DeptTemplateConfig entity = new DeptTemplateConfig();
        entity.setId(1L);
        entity.setDepartmentId("dept-01");
        entity.setRequiredFields("[\"CHIEF_COMPLAINT\"]");
        entity.setTemplateFields("{\"promptMessages\":{}}");
        entity.setVersion(2);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertEquals(1L, entity.getId());
        assertEquals("dept-01", entity.getDepartmentId());
        assertEquals("[\"CHIEF_COMPLAINT\"]", entity.getRequiredFields());
        assertEquals("{\"promptMessages\":{}}", entity.getTemplateFields());
        assertEquals(2, entity.getVersion());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void prePersistShouldSetCreatedAt() {
        DeptTemplateConfig entity = new DeptTemplateConfig();
        entity.prePersist();
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void preUpdateShouldSetUpdatedAt() {
        DeptTemplateConfig entity = new DeptTemplateConfig();
        entity.preUpdate();
        assertNotNull(entity.getUpdatedAt());
    }
}
