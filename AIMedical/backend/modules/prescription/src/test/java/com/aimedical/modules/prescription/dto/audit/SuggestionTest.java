package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SuggestionTest {

    @Test
    void shouldSetAndGetFields() {
        Suggestion suggestion = new Suggestion();
        suggestion.setSuggestionCode("S001");
        suggestion.setSuggestionText("Monitor patient");

        assertEquals("S001", suggestion.getSuggestionCode());
        assertEquals("Monitor patient", suggestion.getSuggestionText());
    }
}
