package com.aimedical.modules.ai.impl.degradation;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import com.aimedical.modules.ai.api.degradation.DegradationContext;

import static org.junit.jupiter.api.Assertions.*;

class NoOpDegradationStrategyTest {

    private final NoOpDegradationStrategy strategy = new NoOpDegradationStrategy();

    @Test
    void shouldAlwaysReturnFalse() {
        DegradationContext context = new DegradationContext();
        assertFalse(strategy.shouldDegrade(context));
    }

    @Test
    void shouldBeAnnotatedWithComponent() {
        assertNotNull(NoOpDegradationStrategy.class.getAnnotation(Component.class));
    }
}
