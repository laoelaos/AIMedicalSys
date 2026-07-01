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

    @Test
    void shouldDegradeBasedOnServiceName() {
        DegradationStrategy strategy = context -> "triage".equals(context.getServiceName());
        DegradationContext context = new DegradationContext();
        context.setServiceName("triage");
        assertTrue(strategy.shouldDegrade(context));

        context.setServiceName("prescription");
        assertFalse(strategy.shouldDegrade(context));
    }

    @Test
    void shouldDegradeBasedOnOperationName() {
        DegradationStrategy strategy = context -> "prescriptionCheck".equals(context.getOperationName());
        DegradationContext context = new DegradationContext();
        context.setOperationName("prescriptionCheck");
        assertTrue(strategy.shouldDegrade(context));

        context.setOperationName("prescriptionAssist");
        assertFalse(strategy.shouldDegrade(context));
    }
}
