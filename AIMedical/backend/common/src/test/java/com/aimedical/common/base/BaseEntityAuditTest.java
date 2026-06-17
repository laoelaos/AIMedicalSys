package com.aimedical.common.base;

import com.aimedical.common.config.JpaConfig;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
class BaseEntityAuditTest {

    @SpringBootApplication
    static class TestConfig {}

    @Autowired
    private TestEntityManager em;

    @Entity
    @Table(name = "test_audit_entity")
    static class AuditTestEntity extends BaseEntity {
    }

    @Test
    void shouldAutoFillCreatedAtOnPersist() {
        AuditTestEntity entity = new AuditTestEntity();
        em.persistAndFlush(entity);
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void shouldUpdateUpdatedAtOnUpdate() {
        AuditTestEntity entity = new AuditTestEntity();
        em.persistAndFlush(entity);
        LocalDateTime initialCreatedAt = entity.getCreatedAt();

        entity.setDeleted(true);
        em.persistAndFlush(entity);

        assertEquals(initialCreatedAt, entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }
}
