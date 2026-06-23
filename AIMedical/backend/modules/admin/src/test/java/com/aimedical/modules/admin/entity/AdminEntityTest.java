package com.aimedical.modules.admin.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminEntityTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AdminEntity entity = new AdminEntity();
        assertNotNull(entity);
    }

    @Test
    void shouldExtendBaseEntity() {
        AdminEntity entity = new AdminEntity();
        assertNull(entity.getId());
        assertFalse(entity.getDeleted());
    }
}
