package com.aimedical.modules.consultation;

import com.aimedical.modules.consultation.config.SchedulingRetryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingRetryConfigTest {

    @Test
    void shouldHaveEnableSchedulingAnnotation() {
        assertNotNull(SchedulingRetryConfig.class.getAnnotation(EnableScheduling.class));
    }

    @Test
    void shouldHaveEnableRetryAnnotation() {
        assertNotNull(SchedulingRetryConfig.class.getAnnotation(EnableRetry.class));
    }

    @Test
    void shouldHaveConditionalOnPropertyWithCorrectDefaults() {
        ConditionalOnProperty annotation = SchedulingRetryConfig.class.getAnnotation(ConditionalOnProperty.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{"consultation.scheduling.enabled"}, annotation.name());
        assertEquals("true", annotation.havingValue());
        assertTrue(annotation.matchIfMissing());
    }
}
