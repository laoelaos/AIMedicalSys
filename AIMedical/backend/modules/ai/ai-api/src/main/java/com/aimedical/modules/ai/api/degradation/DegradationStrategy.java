package com.aimedical.modules.ai.api.degradation;

public interface DegradationStrategy {

    boolean shouldDegrade(DegradationContext context);
}
