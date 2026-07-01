package com.aimedical.modules.medicalrecord.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiResultFactory;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenRequest;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse;
import com.aimedical.modules.commonmodule.visit.VisitFacade;
import com.aimedical.modules.medicalrecord.converter.MedicalRecordConverter;
import com.aimedical.modules.medicalrecord.detector.MissingFieldDetector;
import com.aimedical.modules.medicalrecord.dto.FieldMissingHint;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateRequest;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateResponse;
import com.aimedical.modules.medicalrecord.entity.MedicalRecord;
import com.aimedical.modules.medicalrecord.exception.MedicalRecordErrorCode;
import com.aimedical.modules.medicalrecord.repository.MedicalRecordRepository;
import com.aimedical.modules.medicalrecord.service.MedicalRecordService;
import com.aimedical.modules.medicalrecord.template.DepartmentTemplateConfig;
import com.aimedical.modules.medicalrecord.template.TemplateConfigManager;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private static final Logger log = LoggerFactory.getLogger(MedicalRecordServiceImpl.class);

    @Value("${ai.timeout.medical-record-generate:12}")
    private int aiTimeout;

    @Value("${medical-record.visit-facade.timeout:2}")
    private int visitFacadeTimeout;

    private final VisitFacade visitFacade;
    private final TemplateConfigManager templateConfigManager;
    private final AiService aiService;
    private final MissingFieldDetector missingFieldDetector;
    private final MedicalRecordConverter medicalRecordConverter;
    private final MedicalRecordRepository medicalRecordRepository;
    private final java.util.concurrent.ExecutorService medicalRecordExecutor;

    public MedicalRecordServiceImpl(VisitFacade visitFacade, TemplateConfigManager templateConfigManager,
                                     AiService aiService, MissingFieldDetector missingFieldDetector,
                                     MedicalRecordConverter medicalRecordConverter,
                                     MedicalRecordRepository medicalRecordRepository,
                                     java.util.concurrent.ExecutorService medicalRecordExecutor) {
        this.visitFacade = visitFacade;
        this.templateConfigManager = templateConfigManager;
        this.aiService = aiService;
        this.missingFieldDetector = missingFieldDetector;
        this.medicalRecordConverter = medicalRecordConverter;
        this.medicalRecordRepository = medicalRecordRepository;
        this.medicalRecordExecutor = medicalRecordExecutor;
    }

    private static class VisitResolveResult {
        final String visitId;
        final boolean fallback;

        VisitResolveResult(String visitId, boolean fallback) {
            this.visitId = visitId;
            this.fallback = fallback;
        }
    }

    @Override
    public RecordGenerateResponse generate(RecordGenerateRequest request) {
        VisitResolveResult resolveResult = resolveVisitId(request.getEncounterId());
        if (resolveResult == null) {
            RecordGenerateResponse response = new RecordGenerateResponse();
            response.setFields(new HashMap<>());
            response.setMissingFieldHints(Collections.emptyList());
            response.setSuccess(false);
            response.setErrorCode(MedicalRecordErrorCode.MR_GEN_VISIT_NOT_FOUND);
            return response;
        }

        String visitId = resolveResult.visitId;
        boolean visitIdFallback = resolveResult.fallback;

        DepartmentTemplateConfig template = templateConfigManager.getTemplate(request.getDepartmentId());

        MedicalRecordGenRequest aiRequest = medicalRecordConverter.toAiRequest(request);
        AiResult<MedicalRecordGenResponse> aiResult = callAiWithTimeout(aiRequest);

        MedicalRecordGenResponse aiResponse = aiResult.getData();

        List<FieldMissingHint> hints = Collections.emptyList();
        if (aiResponse != null) {
            hints = missingFieldDetector.detect(aiResponse, template);
        }

        MedicalRecord entity = medicalRecordRepository.findByVisitId(visitId)
                .orElseGet(MedicalRecord::new);
        entity.setPatientId(request.getPatientId());
        entity.setVisitId(visitId);
        entity.setDepartmentId(request.getDepartmentId());
        entity.setDoctorId(request.getDoctorId());
        if (aiResponse != null) {
            entity.setContent(medicalRecordConverter.toFieldsMap(aiResponse));
        }
        entity.setVisitIdFallback(visitIdFallback);
        try {
            medicalRecordRepository.save(entity);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict on medical record save", e);
            RecordGenerateResponse response = medicalRecordConverter.toRecordGenerateResponse(aiResult, hints);
            response.setErrorCode(MedicalRecordErrorCode.MR_GEN_CONCURRENT_MODIFICATION);
            response.setFromFallback(visitIdFallback);
            return response;
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent INSERT conflict on medical record for visitId: {}", visitId, e);
            RecordGenerateResponse response = medicalRecordConverter.toRecordGenerateResponse(aiResult, hints);
            response.setErrorCode(MedicalRecordErrorCode.MR_GEN_CONCURRENT_MODIFICATION);
            response.setFromFallback(visitIdFallback);
            return response;
        }

        RecordGenerateResponse response = medicalRecordConverter.toRecordGenerateResponse(aiResult, hints);
        response.setFromFallback(visitIdFallback);
        return response;
    }

    private VisitResolveResult resolveVisitId(String encounterId) {
        if (encounterId == null || encounterId.isEmpty()) {
            return null;
        }
        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(
                    () -> visitFacade.findVisitIdByEncounterId(encounterId), medicalRecordExecutor);
            String visitId = future.get(visitFacadeTimeout, TimeUnit.SECONDS);
            return new VisitResolveResult(visitId, false);
        } catch (TimeoutException e) {
            log.warn("VisitFacade timeout for encounterId: {}, fallback to encounterId", encounterId);
            return new VisitResolveResult(encounterId, true);
        } catch (Exception e) {
            log.warn("VisitFacade failed for encounterId: {}, fallback to encounterId", encounterId, e);
            return new VisitResolveResult(encounterId, true);
        }
    }

    private AiResult<MedicalRecordGenResponse> callAiWithTimeout(MedicalRecordGenRequest aiRequest) {
        CompletableFuture<AiResult<MedicalRecordGenResponse>> future = aiService.generateMedicalRecord(aiRequest);
        try {
            return future.get(aiTimeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("AI 病历生成超时");
            return AiResultFactory.degraded("AI 病历生成超时", "MR_GEN_AI_TIMEOUT", null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AiResultFactory.degraded("AI 病历生成被中断", "MR_GEN_AI_INTERRUPTED", null);
        } catch (ExecutionException e) {
            log.warn("AI 病历生成执行异常", e);
            return AiResultFactory.degraded(e.getCause() != null ? e.getCause().getMessage() : "AI 病历生成执行异常", "MR_GEN_AI_EXECUTION_ERROR", null);
        }
    }
}
