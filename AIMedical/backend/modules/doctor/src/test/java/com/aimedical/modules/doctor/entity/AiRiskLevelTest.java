package com.aimedical.modules.doctor.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiRiskLevelTest {

    @Test
    void shouldHaveCorrectCodes() {
        assertEquals("LOW", AiRiskLevel.LOW.getCode());
        assertEquals("MEDIUM", AiRiskLevel.MEDIUM.getCode());
        assertEquals("HIGH", AiRiskLevel.HIGH.getCode());
    }

    @Test
    void shouldHaveCorrectDescs() {
        assertEquals("低风险", AiRiskLevel.LOW.getDesc());
        assertEquals("中风险", AiRiskLevel.MEDIUM.getDesc());
        assertEquals("高风险", AiRiskLevel.HIGH.getDesc());
    }

    @Test
    void shouldHaveThreeValues() {
        assertEquals(3, AiRiskLevel.values().length);
    }
}
