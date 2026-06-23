package com.aimedical.modules.ai.impl.degradation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.aimedical.modules.ai.api.degradation.DegradationContext;
import com.aimedical.modules.ai.api.degradation.DegradationStrategy;

@Component
@ConditionalOnMissingBean(DegradationStrategy.class)
public class NoOpDegradationStrategy implements DegradationStrategy {

    @Override
    public boolean shouldDegrade(DegradationContext context) {
        return false;
    }
}
