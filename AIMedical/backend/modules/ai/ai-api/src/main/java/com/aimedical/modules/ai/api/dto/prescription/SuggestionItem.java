package com.aimedical.modules.ai.api.dto.prescription;

public class SuggestionItem {

    private String suggestionCode;
    private String suggestionText;

    public SuggestionItem() {
    }

    public String getSuggestionCode() {
        return suggestionCode;
    }

    public void setSuggestionCode(String suggestionCode) {
        this.suggestionCode = suggestionCode;
    }

    public String getSuggestionText() {
        return suggestionText;
    }

    public void setSuggestionText(String suggestionText) {
        this.suggestionText = suggestionText;
    }
}
