package com.aimedical.common.base;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BaseEntityTest {

    private static class TestEntity extends BaseEntity {
    }

    @Test
    void shouldCreateWithDefaultValues() {
        TestEntity entity = new TestEntity();
        assertNull(entity.getId());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
        assertFalse(entity.getDeleted());
    }

    @Test
    void shouldSetAndGetId() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        assertEquals(1L, entity.getId());
    }

    @Test
    void shouldSetAndGetTimestamps() {
        TestEntity entity = new TestEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void shouldSetAndGetDeleted() {
        TestEntity entity = new TestEntity();
        entity.setDeleted(true);
        assertTrue(entity.getDeleted());
    }
}
