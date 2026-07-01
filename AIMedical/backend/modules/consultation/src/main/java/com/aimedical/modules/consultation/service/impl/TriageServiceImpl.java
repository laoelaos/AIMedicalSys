package com.aimedical.modules.consultation.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.modules.consultation.exception.TriageErrorCode;
import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.ai.api.dto.triage.TriageRequest;
import com.aimedical.modules.ai.api.dto.triage.TriageResponse;
import com.aimedical.modules.commonmodule.doctor.AvailableDoctor;
import com.aimedical.modules.commonmodule.doctor.DoctorFacade;
import com.aimedical.modules.consultation.converter.TriageConverter;
import com.aimedical.modules.consultation.dialogue.DialogueSession;
import com.aimedical.modules.consultation.dialogue.DialogueSessionManager;
import com.aimedical.modules.consultation.dto.DialogueCreateRequest;
import com.aimedical.modules.consultation.dto.RecommendedDepartment;
import com.aimedical.modules.consultation.dto.RecommendedDoctor;
import com.aimedical.modules.consultation.entity.TriageRecord;
import com.aimedical.modules.consultation.fallback.DepartmentFallbackProvider;
import com.aimedical.modules.consultation.repository.TriageRecordRepository;
import com.aimedical.modules.consultation.rule.MatchResult;
import com.aimedical.modules.consultation.rule.TriageRuleEngine;
import com.aimedical.modules.consultation.service.TriageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.aimedical.modules.ai.api.AiResultFactory;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class TriageServiceImpl implements TriageService {

    private static final int MAX_AI_FAIL_COUNT = 3;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TriageServiceImpl.class);

    private final AiService aiService;
    private final TriageRuleEngine triageRuleEngine;
    private final DepartmentFallbackProvider fallbackProvider;
    private final DoctorFacade doctorFacade;
    private final DialogueSessionManager sessionManager;
    private final TriageRecordRepository triageRecordRepository;
    private final TriageConverter triageConverter;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final long aiTimeout;
    private final long doctorFacadeTimeout;
    private final Map<String, Lock> triageLocks = new ConcurrentHashMap<>();

    public TriageServiceImpl(AiService aiService, TriageRuleEngine triageRuleEngine,
                              DepartmentFallbackProvider fallbackProvider, DoctorFacade doctorFacade,
                              DialogueSessionManager sessionManager,
                              TriageRecordRepository triageRecordRepository,
                              TriageConverter triageConverter,
                              ObjectMapper objectMapper,
                              PlatformTransactionManager transactionManager,
                              @Value("${ai.timeout.triage:8}") long aiTimeout,
                              @Value("${consultation.doctor-facade.timeout:2}") long doctorFacadeTimeout) {
        this.aiService = aiService;
        this.triageRuleEngine = triageRuleEngine;
        this.fallbackProvider = fallbackProvider;
        this.doctorFacade = doctorFacade;
        this.sessionManager = sessionManager;
        this.triageRecordRepository = triageRecordRepository;
        this.triageConverter = triageConverter;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.aiTimeout = aiTimeout;
        this.doctorFacadeTimeout = doctorFacadeTimeout;
    }

    @Override
    public com.aimedical.modules.consultation.dto.TriageResponse triage(DialogueCreateRequest request) {
        String sessionId = request.getSessionId();

        boolean hasChiefComplaint = request.getChiefComplaint() != null && !request.getChiefComplaint().trim().isEmpty();
        boolean hasAdditional = request.getAdditionalResponses() != null && !request.getAdditionalResponses().isEmpty();
        if (hasChiefComplaint == hasAdditional) {
            throw new BusinessException(TriageErrorCode.TRIAGE_FIELD_COMBINATION_INVALID,
                request.getSessionId());
        }

        DialogueSession session;
        try {
            session = sessionManager.restoreSession(sessionId);
            if (session == null) {
                session = sessionManager.createSession(sessionId);
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException(TriageErrorCode.TRIAGE_SESSION_NOT_FOUND, sessionId);
        }

        if (session.getRuleVersion() == null && request.getRuleVersion() != null) {
            session.setRuleVersion(request.getRuleVersion());
        }
        if (session.getRuleSetId() == null && request.getRuleSetId() != null) {
            session.setRuleSetId(request.getRuleSetId());
        }

        session.setChiefComplaint(request.getChiefComplaint());
        session.setCorrectedChiefComplaint(request.getCorrectedChiefComplaint());
        if (request.getAdditionalResponses() != null) {
            if (session.getAdditionalResponses() == null) {
                session.setAdditionalResponses(new ArrayList<>());
            }
            session.getAdditionalResponses().addAll(request.getAdditionalResponses());
        }
        session.setRoundCount(session.getRoundCount() + 1);

        CompletableFuture<AiResult<TriageResponse>> future = aiService.triage(
                triageConverter.toAiTriageRequest(request, session));

        AiResult<TriageResponse> aiResult;
        try {
            aiResult = future.get(aiTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            aiResult = handleAiFailure(session);
        } catch (ExecutionException e) {
            aiResult = handleAiFailure(session);
        } catch (TimeoutException e) {
            aiResult = handleAiFailure(session);
        }

        List<RecommendedDoctor> doctors = Collections.emptyList();
        List<RecommendedDepartment> departments = Collections.emptyList();

        if (aiResult != null && aiResult.isSuccess()) {
            TriageResponse aiData = aiResult.getData();

            if (aiData.getRecommendedDepartments() != null) {
                departments = aiData.getRecommendedDepartments().stream()
                        .map(d -> new RecommendedDepartment(d.getDepartmentId(), d.getDepartmentName(), d.getScore()))
                        .collect(Collectors.toList());
            }

            doctors = findDoctorsForDepartments(departments);
        }

        if (aiResult == null || !aiResult.isSuccess()) {
            if (aiResult == null || !aiResult.isDegraded()) {
                session.setAiFailCount(session.getAiFailCount() + 1);
            }
            MatchResult matchResult = triageRuleEngine.match(
                    request.getChiefComplaint(), session.getRuleVersion(), session.getRuleSetId());
            List<RecommendedDepartment> ruleMatched = matchResult.getDepartments();
            if (ruleMatched != null && !ruleMatched.isEmpty()) {
                departments = ruleMatched;
            } else {
                departments = fallbackProvider.getFallbackDepartments();
            }
            doctors = findDoctorsForDepartments(departments);

            boolean fallbackHint = session.getAiFailCount() >= MAX_AI_FAIL_COUNT;
            com.aimedical.modules.consultation.dto.TriageResponse fallbackResponse = triageConverter.toFallbackTriageResponse(
                    departments, doctors, sessionId, "AI 服务不可用，已切换至规则引擎降级",
                    matchResult.isRuleVersionMismatch(), fallbackHint);
            saveTriageRecord(request, session, departments, doctors, aiResult, fallbackResponse);
            return fallbackResponse;
        }

        com.aimedical.modules.consultation.dto.TriageResponse result = triageConverter.toTriageResponse(aiResult, doctors, session);
        result.setSessionId(sessionId);
        session.setAiFailCount(0);
        saveTriageRecord(request, session, departments, doctors, aiResult, result);
        return result;
    }

    @Override
    @Transactional
    public com.aimedical.modules.consultation.dto.TriageResponse selectDepartment(
            String sessionId, String departmentId, String departmentName) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        TriageRecord record = triageRecordRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(TriageErrorCode.TRIAGE_SESSION_NOT_FOUND,
                        "TriageRecord not found for sessionId: " + sessionId));

        record.setFinalDepartmentId(departmentId);
        record.setFinalDepartmentName(departmentName);
        triageRecordRepository.save(record);

        return toTriageResponse(record);
    }

    private AiResult<TriageResponse> handleAiFailure(DialogueSession session) {
        session.setAiFailCount(session.getAiFailCount() + 1);
        if (session.getAiFailCount() < MAX_AI_FAIL_COUNT) {
            return AiResultFactory.degraded("AI call failed, attempt " + session.getAiFailCount(), (TriageResponse) null);
        }
        return AiResultFactory.degraded("AI call failed after " + session.getAiFailCount() + " attempts", (TriageResponse) null);
    }

    // TODO: OOD 要求按 score 排序取前 5 名，但 AvailableDoctor 无 score 字段，
    //       当前使用 availableSlotCount 降序作为替代，待 DoctorFacade 补充 score 后修正
    private List<RecommendedDoctor> findDoctorsForDepartments(List<RecommendedDepartment> departments) {
        if (departments == null || departments.isEmpty()) {
            return Collections.emptyList();
        }
        List<RecommendedDoctor> result = new ArrayList<>();
        for (RecommendedDepartment dept : departments) {
            long start = System.currentTimeMillis();
            try {
                CompletableFuture<List<AvailableDoctor>> future = CompletableFuture.supplyAsync(
                        () -> doctorFacade.findAvailableDoctorsByDepartment(dept.getDepartmentId()));
                List<AvailableDoctor> available = future.get(doctorFacadeTimeout, TimeUnit.SECONDS);
                for (AvailableDoctor doc : available) {
                    result.add(new RecommendedDoctor(doc.doctorId(), doc.doctorName(),
                            doc.departmentId(), doc.availableSlotCount(), 0f));
                }
            } catch (Exception e) {
                long elapsedMs = System.currentTimeMillis() - start;
                log.warn("DoctorFacade call failed for department {} after {}ms: {} {}",
                        dept.getDepartmentId(), elapsedMs, e.getClass().getName(), e.getMessage());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return result.stream()
                .sorted((a, b) -> Integer.compare(b.getAvailableSlotCount(), a.getAvailableSlotCount()))
                .limit(5)
                .collect(Collectors.toList());
    }

    private void saveTriageRecord(DialogueCreateRequest request, DialogueSession session,
                                    List<RecommendedDepartment> departments, List<RecommendedDoctor> doctors,
                                    AiResult<TriageResponse> aiResult,
                                    com.aimedical.modules.consultation.dto.TriageResponse response) {
        Lock lock = triageLocks.computeIfAbsent(request.getSessionId(), k -> new ReentrantLock());
        lock.lock();
        try {
            String departmentsJson = null;
            String doctorsJson = null;
            try {
                if (departments != null && !departments.isEmpty()) {
                    departmentsJson = objectMapper.writeValueAsString(departments);
                }
                if (doctors != null && !doctors.isEmpty()) {
                    doctorsJson = objectMapper.writeValueAsString(doctors);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize triage record JSON fields for sessionId: {}, departments={}, doctors={}",
                        request.getSessionId(),
                        departments != null ? departmentsJson : "null",
                        doctors != null ? doctorsJson : "null",
                        e);
            }

            String finalDepartmentsJson = departmentsJson;
            String finalDoctorsJson = doctorsJson;
            transactionTemplate.execute(status -> {
                Optional<TriageRecord> existing = triageRecordRepository.findBySessionId(request.getSessionId());

                TriageRecord record;
                if (existing.isPresent()) {
                    record = existing.get();
                } else {
                    record = new TriageRecord();
                }

                record.setSessionId(request.getSessionId());
                record.setPatientId(request.getPatientId());
                record.setChiefComplaint(request.getChiefComplaint());
                record.setTriageTime(LocalDateTime.now());
                record.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint());
                record.setRuleVersion(request.getRuleVersion());
                record.setRuleSetId(request.getRuleSetId());
                record.setConfidence(response.getConfidence());

                record.setDegraded(response.isDegraded());

                if (finalDepartmentsJson != null) {
                    if (response.isDegraded()) {
                        record.setRuleMatchedDepartments(finalDepartmentsJson);
                    } else {
                        record.setAiRecommendedDepartments(finalDepartmentsJson);
                    }
                }
                if (finalDoctorsJson != null) {
                    record.setRecommendedDoctors(finalDoctorsJson);
                }

                triageRecordRepository.save(record);
                return null;
            });
        } finally {
            lock.unlock();
            if (triageLocks.size() > 1000) {
                triageLocks.remove(request.getSessionId());
            }
        }
    }

    private com.aimedical.modules.consultation.dto.TriageResponse toTriageResponse(TriageRecord record) {
        com.aimedical.modules.consultation.dto.TriageResponse response = new com.aimedical.modules.consultation.dto.TriageResponse();
        response.setSessionId(record.getSessionId());
        response.setReason("Department selected");
        response.setConfidence(record.getConfidence());
        response.setDegraded(Boolean.TRUE.equals(record.getDegraded()));
        return response;
    }
}
