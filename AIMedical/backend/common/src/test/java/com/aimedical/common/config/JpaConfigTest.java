package com.aimedical.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import static org.junit.jupiter.api.Assertions.*;

class JpaConfigTest {

    @Test
    void shouldBeConfigurationClass() {
        assertNotNull(JpaConfig.class.getAnnotation(Configuration.class));
    }

    @Test
    void shouldEnableJpaAuditing() {
        assertNotNull(JpaConfig.class.getAnnotation(EnableJpaAuditing.class));
    }

    @Test
    void shouldInstantiateWithoutError() {
        assertDoesNotThrow(JpaConfig::new);
    }
}
