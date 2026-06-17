package com.aimedical.modules.ai.api.degradation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DegradationStrategyTest {

    @Test
    void shouldCreateDegradationContextWithDefaultConstructor() {
        DegradationContext context = new DegradationContext();
        assertNotNull(context);
    }

    @Test
    void shouldReturnTrueWhenShouldDegrade() {
        DegradationStrategy strategy = context -> true;
        DegradationContext context = new DegradationContext();
        assertTrue(strategy.shouldDegrade(context));
    }

    @Test
    void shouldReturnFalseWhenShouldNotDegrade() {
        DegradationStrategy strategy = context -> false;
        DegradationContext context = new DegradationContext();
        assertFalse(strategy.shouldDegrade(context));
    }

    @Test
    void shouldUseContextToDetermineDegradation() {
        DegradationStrategy strategy = context -> true;
        assertTrue(strategy.shouldDegrade(new DegradationContext()));
    }
}
