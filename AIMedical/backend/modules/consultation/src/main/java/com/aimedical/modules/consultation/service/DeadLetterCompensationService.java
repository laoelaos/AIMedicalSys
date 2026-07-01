package com.aimedical.modules.consultation.service;

import com.aimedical.modules.consultation.entity.DeadLetterEvent;
import com.aimedical.modules.consultation.repository.DeadLetterEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DeadLetterCompensationService {

    private final DeadLetterEventRepository deadLetterEventRepository;
    private final TriageService triageService;
    private final ObjectMapper objectMapper;

    public DeadLetterCompensationService(DeadLetterEventRepository deadLetterEventRepository,
                                          TriageService triageService,
                                          ObjectMapper objectMapper) {
        this.deadLetterEventRepository = deadLetterEventRepository;
        this.triageService = triageService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 1800000)
    public void compensateDeadLetters() {
        java.util.List<DeadLetterEvent> events = deadLetterEventRepository.findByCompensableEvents("FAILED");
        for (DeadLetterEvent event : events) {
            if (event.getRetryCount() >= event.getMaxRetryCount()) {
                event.setState("EXPIRED");
                deadLetterEventRepository.save(event);
                continue;
            }
            try {
                Map<String, String> payload = objectMapper.readValue(
                        event.getEventPayload(), new TypeReference<Map<String, String>>() {});
                String sessionId = payload.get("sessionId");
                String departmentId = payload.get("departmentId");
                String departmentName = payload.get("departmentName");

                triageService.selectDepartment(sessionId, departmentId, departmentName);

                event.setState("COMPENSATED");
                deadLetterEventRepository.save(event);
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= event.getMaxRetryCount()) {
                    event.setState("EXPIRED");
                }
                deadLetterEventRepository.save(event);
            }
        }
    }
}
