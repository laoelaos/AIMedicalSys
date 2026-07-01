package com.aimedical.modules.prescription.service.audit.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiResultFactory;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.commonmodule.drug.DrugFacade;
import com.aimedical.modules.ai.api.dto.prescription.AlertItem;
import com.aimedical.modules.ai.api.dto.prescription.DrugInteractionItem;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckResponse;
import com.aimedical.modules.commonmodule.auth.CurrentUser;
import com.aimedical.modules.prescription.context.DosageAlert;
import com.aimedical.modules.prescription.context.PrescriptionDraftContext;
import com.aimedical.modules.prescription.converter.AuditConverter;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.AuditResponse;
import com.aimedical.modules.prescription.dto.audit.BlockResponse;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.dto.audit.SubmitRequest;
import com.aimedical.modules.prescription.dto.audit.AlertSeverity;
import com.aimedical.modules.prescription.dto.audit.AuditAlert;
import com.aimedical.modules.prescription.dto.audit.AuditIssue;
import com.aimedical.modules.prescription.dto.audit.SubmitResponse;
import com.aimedical.modules.prescription.dto.audit.WarnAlert;
import com.aimedical.modules.prescription.dto.audit.WarnResult;
import com.aimedical.modules.prescription.entity.AuditRecord;
import com.aimedical.modules.prescription.PrescriptionErrorCode;
import com.aimedical.modules.prescription.repository.AuditRecordRepository;
import com.aimedical.modules.prescription.rule.LocalRuleEngine;
import com.aimedical.modules.prescription.rule.LocalRuleResult;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import com.aimedical.modules.prescription.service.audit.PrescriptionAuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class PrescriptionAuditServiceImpl implements PrescriptionAuditService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionAuditServiceImpl.class);

    private final AiService aiService;
    private final LocalRuleEngine localRuleEngine;
    private final AuditRecordRepository auditRecordRepository;
    private final AuditConverter auditConverter;
    private final PrescriptionDraftContext prescriptionDraftContext;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;
    private final long aiTimeout;
    private final DrugFacade drugFacade;
    private final ConcurrentHashMap<String, ReentrantLock> submitLocks = new ConcurrentHashMap<>();

    public PrescriptionAuditServiceImpl(AiService aiService, LocalRuleEngine localRuleEngine,
                                         AuditRecordRepository auditRecordRepository,
                                         AuditConverter auditConverter,
                                         PrescriptionDraftContext prescriptionDraftContext,
                                         CurrentUser currentUser,
                                         ObjectMapper objectMapper,
                                          @Value("${ai.timeout.prescription-audit:6}") long aiTimeout,
                                          DrugFacade drugFacade) {
        this.aiService = aiService;
        this.localRuleEngine = localRuleEngine;
        this.auditRecordRepository = auditRecordRepository;
        this.auditConverter = auditConverter;
        this.prescriptionDraftContext = prescriptionDraftContext;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
        this.aiTimeout = aiTimeout;
        this.drugFacade = drugFacade;
    }

    @Override
    @Transactional
    public AuditResponse audit(AuditRequest request) {
        CompletableFuture<AiResult<PrescriptionCheckResponse>> future = aiService.prescriptionCheck(
                auditConverter.toAiPrescriptionCheckRequest(request));

        AiResult<PrescriptionCheckResponse> aiResult;
        try {
            aiResult = future.get(aiTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            aiResult = AiResultFactory.failure("RX_AUDIT_AI_INTERRUPTED");
        } catch (ExecutionException e) {
            aiResult = AiResultFactory.failure("RX_AUDIT_AI_EXECUTION_ERROR");
        } catch (TimeoutException e) {
            aiResult = AiResultFactory.failure("RX_AUDIT_AI_TIMEOUT");
        }

        boolean fromFallback = false;
        AuditResponse response;

        if (aiResult != null && aiResult.isSuccess() && aiResult.getData() != null) {
            response = auditConverter.toAuditResponse(aiResult);
        } else {
            log.warn("AI service unavailable, switching to local rule engine. aiResult={}",
                aiResult != null ? aiResult.getErrorCode() : "null");
            fromFallback = true;
            List<LocalRuleResult> ruleResults = localRuleEngine.check(request);
            AuditRiskLevel aggregated = aggregateRiskLevel(ruleResults);
            response = new AuditResponse();
            response.setRiskLevel(aggregated);
            List<AuditAlert> alerts = new ArrayList<>();
            for (LocalRuleResult result : ruleResults) {
                if (!result.isPassed()) {
                    alerts.add(new AuditAlert(result.getRuleId(), result.getMessage(), toAlertSeverity(result.getSeverity())));
                }
            }
            response.setAlerts(alerts);
            response.setInteractions(Collections.emptyList());
            response.setSuggestions(Collections.emptyList());
            response.setFromFallback(true);
        }

        String originalPrescriptionJson;
        try {
            originalPrescriptionJson = objectMapper.writeValueAsString(request.getPrescriptionItems());
        } catch (JsonProcessingException e) {
            originalPrescriptionJson = "[]";
        }

        persistAuditRecord(request, response, originalPrescriptionJson, fromFallback,
                aiResult != null && aiResult.isSuccess() ? aiResult : null);

        return response;
    }

    @Override
    @Transactional
    public SubmitResponse submit(SubmitRequest request) {
        String lockKey = request.getPrescriptionId();
        ReentrantLock lock = submitLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();
        try {
            return doSubmit(request);
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                submitLocks.remove(lockKey);
            }
        }
    }

    private SubmitResponse doSubmit(SubmitRequest request) {
        PrescriptionDraftContext.SnapshotResult snapshot = prescriptionDraftContext.snapshotCriticalAlerts(request.getPrescriptionId());
        if (snapshot.hasAlerts) {
            List<String> reasons = snapshot.alerts.stream()
                    .map(DosageAlert::getMessage)
                    .collect(Collectors.toList());
            if (reasons.isEmpty()) reasons = List.of("Critical dosage alerts detected");
            BlockResponse blockInfo = new BlockResponse(
                    reasons,
                    "RX_BLOCK_CRITICAL_DOSE",
                    LocalDateTime.now());
            SubmitResponse resp = new SubmitResponse();
            resp.setSubmitted(false);
            resp.setBlockInfo(blockInfo);
            return resp;
        }

        Optional<AuditRecord> latestRecordOpt = auditRecordRepository
                .findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc(request.getPrescriptionId());
        if (latestRecordOpt.isPresent()) {
            AuditRecord latestRecord = latestRecordOpt.get();
            if ("BLOCK".equals(latestRecord.getRiskLevel())) {
                BlockResponse blockInfo = new BlockResponse(
                        List.of("Prescription audit blocked"),
                        "RX_BLOCK_AUDIT",
                        LocalDateTime.now());
                SubmitResponse resp = new SubmitResponse();
                resp.setSubmitted(false);
                resp.setBlockInfo(blockInfo);
                return resp;
            }
        }

        return handleStepThree(request, latestRecordOpt);
    }

    private SubmitResponse handleStepThree(SubmitRequest request, Optional<AuditRecord> latestRecordOpt) {
        AuditRecord latestRecord = latestRecordOpt.orElse(null);

        if (latestRecord == null) {
            AuditRequest auditReq = new AuditRequest();
            auditReq.setPrescriptionId(request.getPrescriptionId());
            auditReq.setPrescriptionItems(request.getPrescriptionItems());
            AuditResponse auditResp = audit(auditReq);
            return buildStepThreeResponse(request, auditResp, null);
        }

        AuditRiskLevel riskLevel;
        try {
            riskLevel = AuditRiskLevel.valueOf(latestRecord.getRiskLevel());
        } catch (IllegalArgumentException e) {
            riskLevel = AuditRiskLevel.PASS;
        }

        if (!request.isForceSubmit() && riskLevel == AuditRiskLevel.PASS) {
            SubmitResponse resp = new SubmitResponse();
            resp.setSubmitted(true);
            resp.setPrescriptionOrderId("RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            return resp;
        }

        if (!request.isForceSubmit() && riskLevel == AuditRiskLevel.WARN) {
            if (!prescriptionsMatch(latestRecord.getOriginalPrescription(), request.getPrescriptionItems())) {
                SubmitResponse resp = new SubmitResponse();
                resp.setSubmitted(false);
                resp.setErrorCode(PrescriptionErrorCode.RX_AUDIT_PRESCRIPTION_MODIFIED.getCode());
                return resp;
            }
            SubmitResponse resp = new SubmitResponse();
            resp.setSubmitted(false);
            resp.setWarnResult(buildWarnResultFromRecord(latestRecord));
            resp.setErrorCode(null);
            return resp;
        }

        if (request.isForceSubmit()) {
            if (riskLevel != AuditRiskLevel.WARN) {
                SubmitResponse resp = new SubmitResponse();
                resp.setSubmitted(false);
                resp.setErrorCode(PrescriptionErrorCode.RX_AUDIT_FORCE_SUBMIT_INVALID.getCode());
                return resp;
            }

            if (request.getAuditRecordId() == null || !request.getAuditRecordId().equals(latestRecord.getId())) {
                SubmitResponse resp = new SubmitResponse();
                resp.setSubmitted(false);
                resp.setErrorCode(PrescriptionErrorCode.RX_AUDIT_FORCE_SUBMIT_INVALID.getCode());
                return resp;
            }

            if (!prescriptionsMatch(latestRecord.getOriginalPrescription(), request.getPrescriptionItems())) {
                SubmitResponse resp = new SubmitResponse();
                resp.setSubmitted(false);
                resp.setErrorCode(PrescriptionErrorCode.RX_AUDIT_PRESCRIPTION_MODIFIED.getCode());
                return resp;
            }

            try {
                String orderId = "RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                latestRecord.setPrescriptionOrderId(orderId);
                latestRecord.setForceSubmitted(true);
                latestRecord.setForceSubmitTime(LocalDateTime.now());

                List<AuditRecord> orderRecords = auditRecordRepository
                        .findByPrescriptionOrderIdAndIsLatestTrue(orderId);
                for (AuditRecord orderRecord : orderRecords) {
                    orderRecord.setLatest(false);
                }
                if (!orderRecords.isEmpty()) {
                    auditRecordRepository.saveAll(orderRecords);
                }

                auditRecordRepository.save(latestRecord);

                SubmitResponse resp = new SubmitResponse();
                resp.setSubmitted(true);
                resp.setPrescriptionOrderId(orderId);
                return resp;
            } catch (ObjectOptimisticLockingFailureException e) {
                SubmitResponse resp = new SubmitResponse();
                resp.setSubmitted(false);
                resp.setErrorCode(PrescriptionErrorCode.RX_AUDIT_CONCURRENT_SUBMIT.getCode());
                return resp;
            }
        }

        SubmitResponse fallbackResp = new SubmitResponse();
        fallbackResp.setSubmitted(true);
        fallbackResp.setPrescriptionOrderId("RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return fallbackResp;
    }

    private SubmitResponse buildStepThreeResponse(SubmitRequest request, AuditResponse auditResp, AuditRecord record) {
        AuditRiskLevel riskLevel = auditResp.getRiskLevel();
        if (riskLevel == AuditRiskLevel.PASS) {
            SubmitResponse resp = new SubmitResponse();
            resp.setSubmitted(true);
            resp.setPrescriptionOrderId("RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            return resp;
        }
        if (riskLevel == AuditRiskLevel.BLOCK) {
            SubmitResponse resp = new SubmitResponse();
            resp.setSubmitted(false);
            BlockResponse blockInfo = new BlockResponse(
                List.of("Prescription audit blocked"),
                "RX_BLOCK_AUDIT",
                LocalDateTime.now());
            resp.setBlockInfo(blockInfo);
            return resp;
        }
        if (riskLevel == AuditRiskLevel.WARN) {
            if (record != null && !prescriptionsMatch(record.getOriginalPrescription(), request.getPrescriptionItems())) {
                SubmitResponse resp = new SubmitResponse();
                resp.setSubmitted(false);
                resp.setErrorCode(PrescriptionErrorCode.RX_AUDIT_PRESCRIPTION_MODIFIED.getCode());
                return resp;
            }
            if (!request.isForceSubmit()) {
                SubmitResponse resp = new SubmitResponse();
                resp.setSubmitted(false);
                resp.setWarnResult(buildWarnResultFromAuditResponse(auditResp, record, request));
                resp.setErrorCode(null);
                return resp;
            }
        }
        SubmitResponse resp = new SubmitResponse();
        resp.setSubmitted(true);
        resp.setPrescriptionOrderId("RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return resp;
    }

    @Override
    @Transactional
    public void revoke(Long auditId) {
        AuditRecord record = auditRecordRepository.findById(auditId)
                .orElseThrow(() -> new BusinessException(PrescriptionErrorCode.RX_AUDIT_REVOKE_NOT_FOUND));

        if (!record.isLatest()) {
            throw new BusinessException(PrescriptionErrorCode.RX_AUDIT_REVOKE_ALREADY_REVOKED);
        }

        AuditRiskLevel riskLevel;
        try {
            riskLevel = AuditRiskLevel.valueOf(record.getRiskLevel());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(PrescriptionErrorCode.RX_AUDIT_REVOKE_NOT_WARN);
        }

        if (riskLevel != AuditRiskLevel.WARN) {
            throw new BusinessException(PrescriptionErrorCode.RX_AUDIT_REVOKE_NOT_WARN);
        }

        record.setLatest(false);
        auditRecordRepository.save(record);
    }

    private void persistAuditRecord(AuditRequest request, AuditResponse response,
                                     String originalPrescriptionJson, boolean fromFallback,
                                     AiResult<PrescriptionCheckResponse> aiResult) {
        List<AuditRecord> existingRecords = auditRecordRepository
                .findByPrescriptionIdAndIsLatestTrue(request.getPrescriptionId());
        for (AuditRecord existing : existingRecords) {
            existing.setLatest(false);
        }
        auditRecordRepository.saveAll(existingRecords);

        AuditRecord record = new AuditRecord();
        record.setPrescriptionId(request.getPrescriptionId());
        record.setDoctorId(String.valueOf(currentUser.getUserId()));
        record.setPatientId(request.getPatientInfo() != null ? request.getPatientInfo().getPatientId() : null);
        record.setAuditTime(LocalDateTime.now());
        record.setFromFallback(fromFallback);
        record.setOriginalPrescription(originalPrescriptionJson);
        record.setRiskLevel(response.getRiskLevel() != null ? response.getRiskLevel().name() : "PASS");

        int nextSequence = existingRecords.stream()
                .mapToInt(AuditRecord::getAuditSequence)
                .max()
                .orElse(0) + 1;
        record.setAuditSequence(nextSequence);
        record.setLatest(true);

        if (aiResult != null && aiResult.isSuccess()) {
            try {
                record.setAiResult(objectMapper.writeValueAsString(aiResult.getData()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize AI result", e);
            }
        }

        List<AuditIssue> issues = new ArrayList<>();
        if (fromFallback && response.getAlerts() != null) {
            for (AuditAlert alert : response.getAlerts()) {
                AuditIssue issue = new AuditIssue();
                issue.setFieldName(null);
                issue.setIssueDescription(alert.getAlertMessage());
                issue.setRuleId(alert.getAlertCode());
                issue.setSeverity(alert.getSeverity());
                issues.add(issue);
            }
        } else if (aiResult != null && aiResult.isSuccess()) {
            if (aiResult.getData().getAlerts() != null) {
                for (AlertItem alert : aiResult.getData().getAlerts()) {
                    AuditIssue issue = new AuditIssue();
                    issue.setFieldName(null);
                    issue.setIssueDescription(alert.getAlertMessage());
                    issue.setRuleId(alert.getAlertCode());
                    issue.setSeverity(toAlertSeverity(alert.getSeverity()));
                    issues.add(issue);
                }
            }
            if (aiResult.getData().getInteractions() != null) {
                for (DrugInteractionItem item : aiResult.getData().getInteractions()) {
                    AuditIssue issue = new AuditIssue();
                    issue.setFieldName(item.getDrugPair());
                    issue.setIssueDescription(item.getDescription());
                    issue.setRuleId("DRUG_INTERACTION_" + item.getDrugPair());
                    issue.setSeverity(toAlertSeverity(item.getSeverity()));
                    issues.add(issue);
                }
            }
        }
        if (!issues.isEmpty()) {
            try {
                record.setAuditIssues(objectMapper.writeValueAsString(issues));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize audit issues", e);
            }
        }

        auditRecordRepository.save(record);
    }

    private static AlertSeverity toAlertSeverity(AuditRiskLevel riskLevel) {
        if (riskLevel == null) return AlertSeverity.INFO;
        switch (riskLevel) {
            case BLOCK: return AlertSeverity.CRITICAL;
            case WARN:  return AlertSeverity.WARNING;
            default:    return AlertSeverity.INFO;
        }
    }

    private static AlertSeverity toAlertSeverity(String severity) {
        if (severity == null) return AlertSeverity.INFO;
        switch (severity.toUpperCase()) {
            case "CRITICAL": return AlertSeverity.CRITICAL;
            case "WARNING":
            case "WARN":     return AlertSeverity.WARNING;
            default:         return AlertSeverity.INFO;
        }
    }

    private AuditRiskLevel aggregateRiskLevel(List<LocalRuleResult> ruleResults) {
        boolean hasWarn = false;
        for (LocalRuleResult result : ruleResults) {
            if (!result.isPassed() && result.getSeverity() == AuditRiskLevel.BLOCK) {
                return AuditRiskLevel.BLOCK;
            }
            if (!result.isPassed() && result.getSeverity() == AuditRiskLevel.WARN) {
                hasWarn = true;
            }
        }
        return hasWarn ? AuditRiskLevel.WARN : AuditRiskLevel.PASS;
    }

    private boolean prescriptionsMatch(String originalPrescriptionJson, List<PrescriptionItem> currentItems) {
        if (originalPrescriptionJson == null || originalPrescriptionJson.isBlank()) {
            return currentItems == null || currentItems.isEmpty();
        }
        try {
            com.fasterxml.jackson.core.type.TypeReference<List<PrescriptionItem>> typeRef =
                    new com.fasterxml.jackson.core.type.TypeReference<>() {};
            List<PrescriptionItem> originalItems = objectMapper.readValue(originalPrescriptionJson, typeRef);
            return doPrescriptionsMatch(originalItems, currentItems);
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private boolean doPrescriptionsMatch(List<PrescriptionItem> original, List<PrescriptionItem> current) {
        if (original == null && current == null) return true;
        if (original == null || current == null) return false;
        if (original.size() != current.size()) return false;

        Set<String> originalSet = original.stream()
                .map(this::itemToComparisonKey)
                .collect(Collectors.toSet());
        Set<String> currentSet = current.stream()
                .map(this::itemToComparisonKey)
                .collect(Collectors.toSet());
        return originalSet.equals(currentSet);
    }

    private String itemToComparisonKey(PrescriptionItem item) {
        return (item.getDrugId() != null ? item.getDrugId() : "")
                + "|" + item.getDose()
                + "|" + (item.getFrequency() != null ? item.getFrequency() : "")
                + "|" + (item.getDuration() != null ? item.getDuration() : "")
                + "|" + (item.getRoute() != null ? item.getRoute() : "");
    }

    private WarnResult buildWarnResultFromRecord(AuditRecord record) {
        List<WarnAlert> alerts = new ArrayList<>();
        String auditIssuesJson = record.getAuditIssues();
        if (auditIssuesJson != null && !auditIssuesJson.isBlank()) {
            try {
                List<AuditIssue> issues = objectMapper.readValue(auditIssuesJson,
                        new TypeReference<List<AuditIssue>>() {});
                for (AuditIssue issue : issues) {
                    alerts.add(new WarnAlert(issue.getRuleId(), issue.getIssueDescription(), issue.getSeverity()));
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse auditIssues JSON, returning empty alerts", e);
            }
        }
        String hash = computePrescriptionHash(record.getOriginalPrescription());
        return new WarnResult(AuditRiskLevel.WARN, alerts, record.getId(), hash);
    }

    private WarnResult buildWarnResultFromAuditResponse(AuditResponse auditResp, AuditRecord record, SubmitRequest request) {
        List<WarnAlert> alerts = new ArrayList<>();
        if (auditResp.getAlerts() != null) {
            for (AuditAlert alert : auditResp.getAlerts()) {
                alerts.add(new WarnAlert(alert.getAlertCode(), alert.getAlertMessage(), alert.getSeverity()));
            }
        }
        Long auditRecordId = (record != null) ? record.getId() : null;
        String hash;
        if (record != null) {
            hash = computePrescriptionHash(record.getOriginalPrescription());
        } else {
            hash = computePrescriptionHash(request.getPrescriptionItems());
        }
        return new WarnResult(AuditRiskLevel.WARN, alerts, auditRecordId, hash);
    }

    private String computePrescriptionHash(String originalPrescriptionJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(originalPrescriptionJson.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String computePrescriptionHash(List<PrescriptionItem> items) {
        try {
            String json = objectMapper.writeValueAsString(items);
            return computePrescriptionHash(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
