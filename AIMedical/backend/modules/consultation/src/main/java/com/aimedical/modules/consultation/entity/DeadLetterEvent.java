package com.aimedical.modules.consultation.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_event")
public class DeadLetterEvent extends BaseEntity {

    @Column(columnDefinition = "TEXT", nullable = false)
    private String eventPayload;

    @Column(length = 500, nullable = false)
    private String failReason;

    private LocalDateTime failTime;

    @Column(length = 20)
    private String state = "FAILED";

    private Integer retryCount = 0;

    private Integer maxRetryCount = 3;

    public String getEventPayload() {
        return eventPayload;
    }

    public void setEventPayload(String eventPayload) {
        this.eventPayload = eventPayload;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public LocalDateTime getFailTime() {
        return failTime;
    }

    public void setFailTime(LocalDateTime failTime) {
        this.failTime = failTime;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
}
