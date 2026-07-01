package com.aimedical.modules.consultation.event;

import com.aimedical.modules.commonmodule.event.RegistrationEvent;
import com.aimedical.modules.consultation.entity.DeadLetterEvent;
import com.aimedical.modules.consultation.repository.DeadLetterEventRepository;
import com.aimedical.modules.consultation.repository.TriageRecordRepository;
import com.aimedical.modules.consultation.service.TriageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

@Component
public class RegistrationEventListener {

    private static final Logger log = LoggerFactory.getLogger(RegistrationEventListener.class);

    private final TriageRecordRepository triageRecordRepository;
    private final DeadLetterEventRepository deadLetterEventRepository;
    private final ObjectMapper objectMapper;
    private final TriageService triageService;

    public RegistrationEventListener(TriageRecordRepository triageRecordRepository,
                                      DeadLetterEventRepository deadLetterEventRepository,
                                      ObjectMapper objectMapper,
                                      TriageService triageService) {
        this.triageRecordRepository = triageRecordRepository;
        this.deadLetterEventRepository = deadLetterEventRepository;
        this.objectMapper = objectMapper;
        this.triageService = triageService;
    }

    @EventListener
    @Transactional
    @Retryable(retryFor = {DataAccessException.class, TimeoutException.class},
               noRetryFor = {IllegalArgumentException.class, NullPointerException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void handleRegistrationEvent(RegistrationEvent event) {
        if (event.getSessionId() == null) {
            log.warn("RegistrationEvent received with null sessionId, skipping");
            return;
        }
        triageRecordRepository.findBySessionId(event.getSessionId()).ifPresent(record -> {
            if (record.getFinalDepartmentId() == null) {
                triageService.selectDepartment(event.getSessionId(), event.getDepartmentId(), event.getDepartmentName());
            }
        });
    }

    @Recover
    @Transactional
    public void recover(Exception e, RegistrationEvent event) {
        DeadLetterEvent deadLetter = new DeadLetterEvent();
        try {
            deadLetter.setEventPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            String sid = event.getSessionId() != null ? event.getSessionId() : "unknown";
            deadLetter.setEventPayload("{\"sessionId\":\"" + sid + "\"}");
        }
        deadLetter.setFailReason(e.getMessage() != null ? e.getMessage() : "Unknown failure reason");
        deadLetter.setFailTime(LocalDateTime.now());
        deadLetter.setState("FAILED");
        deadLetter.setRetryCount(0);
        deadLetter.setMaxRetryCount(3);
        deadLetterEventRepository.save(deadLetter);
    }
}
