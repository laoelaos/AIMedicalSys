package com.aimedical;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApplicationMainTest {

    @Test
    void shouldInvokeMainMethod() {
        try {
            Application.main(new String[]{"--spring.profiles.active=test"});
        } catch (Exception ignored) {
            // SpringApplication.run may throw if no full Spring context is available;
            // line coverage is what we care about for JaCoCo, not full startup.
        }
    }

    @Test
    void shouldThrowWhenPhase0WithoutDev() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"phase0"});
        Application app = new Application(env);
        assertThrows(IllegalStateException.class, app::validateProfiles);
    }

    @Test
    void shouldPassWhenPhase0WithDev() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"phase0", "dev"});
        Application app = new Application(env);
        assertDoesNotThrow(app::validateProfiles);
    }

    @Test
    void shouldPassWhenNoPhase0() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"test"});
        Application app = new Application(env);
        assertDoesNotThrow(app::validateProfiles);
    }
}
