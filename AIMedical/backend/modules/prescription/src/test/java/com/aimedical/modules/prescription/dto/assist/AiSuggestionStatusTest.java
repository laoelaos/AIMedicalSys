package com.aimedical.modules.prescription.dto.assist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiSuggestionStatusTest {

    @Test
    void shouldHaveAllValues() {
        assertEquals(5, AiSuggestionStatus.values().length);
        assertEquals(AiSuggestionStatus.PENDING, AiSuggestionStatus.valueOf("PENDING"));
        assertEquals(AiSuggestionStatus.PROCESSING, AiSuggestionStatus.valueOf("PROCESSING"));
        assertEquals(AiSuggestionStatus.COMPLETED, AiSuggestionStatus.valueOf("COMPLETED"));
        assertEquals(AiSuggestionStatus.FAILED, AiSuggestionStatus.valueOf("FAILED"));
        assertEquals(AiSuggestionStatus.TIMEOUT, AiSuggestionStatus.valueOf("TIMEOUT"));
    }
}
