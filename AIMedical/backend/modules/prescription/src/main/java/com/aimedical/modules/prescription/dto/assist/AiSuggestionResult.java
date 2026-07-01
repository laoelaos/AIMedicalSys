package com.aimedical.modules.prescription.dto.assist;

import com.aimedical.modules.commonmodule.store.SuggestionStoreEntry;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class AiSuggestionResult implements SuggestionStoreEntry {

    private String taskId;
    private String suggestion;
    private AiSuggestionStatus status;
    private LocalDateTime createTime;
    private String failReason;
    private boolean consumed;
    private String partialData;

    public AiSuggestionResult() {
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public AiSuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(AiSuggestionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public String getPartialData() {
        return partialData;
    }

    public void setPartialData(String partialData) {
        this.partialData = partialData;
    }

    @Override
    public String getStatusName() {
        return status != null ? status.name() : null;
    }

    @Override
    public Instant getTimestamp() {
        return createTime != null ? createTime.toInstant(ZoneOffset.UTC) : null;
    }
}
