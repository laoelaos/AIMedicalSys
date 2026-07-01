package com.aimedical.modules.medicalrecord.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateConfigChangeEventTest {

    @Test
    void shouldCreateEventWithDepartmentId() {
        TemplateConfigChangeEvent event = new TemplateConfigChangeEvent("dept-001");
        assertEquals("dept-001", event.getDepartmentId());
    }

    @Test
    void shouldCreateEventWithNullDepartmentId() {
        TemplateConfigChangeEvent event = new TemplateConfigChangeEvent(null);
        assertNull(event.getDepartmentId());
    }

    @Test
    void shouldHandleEmptyStringDepartmentId() {
        TemplateConfigChangeEvent event = new TemplateConfigChangeEvent("");
        assertEquals("", event.getDepartmentId());
    }
}
