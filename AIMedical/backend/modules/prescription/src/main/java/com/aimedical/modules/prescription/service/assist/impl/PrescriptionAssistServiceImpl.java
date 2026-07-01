package com.aimedical.modules.prescription.service.assist.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.commonmodule.drug.DrugFacade;
import com.aimedical.modules.commonmodule.store.SuggestionStore;
import com.aimedical.modules.prescription.PrescriptionErrorCode;
import com.aimedical.modules.prescription.context.PrescriptionDraftContext;
import com.aimedical.modules.prescription.converter.AssistConverter;
import com.aimedical.modules.prescription.dto.assist.AiSuggestionResult;
import com.aimedical.modules.prescription.dto.assist.AiSuggestionStatus;
import com.aimedical.modules.prescription.dto.assist.AllergyWarningItem;
import com.aimedical.modules.prescription.dto.assist.AllergyWarningSeverity;
import com.aimedical.modules.prescription.dto.assist.DosageAlertLevel;
import com.aimedical.modules.prescription.dto.assist.DosageCheckRequest;
import com.aimedical.modules.prescription.dto.assist.DoseWarning;
import com.aimedical.modules.prescription.dto.assist.DosageCheckResponse;
import com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequest;
import com.aimedical.modules.prescription.dto.assist.PrescriptionAssistResponse;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.rule.AllergyCheckRule;
import com.aimedical.modules.prescription.rule.LocalRuleResult;
import com.aimedical.modules.prescription.service.assist.DedupTaskScheduler;
import com.aimedical.modules.prescription.service.assist.DosageThresholdService;
import com.aimedical.modules.prescription.service.assist.PrescriptionAssistService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class PrescriptionAssistServiceImpl implements PrescriptionAssistService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionAssistServiceImpl.class);

    private final AiService aiService;
    private final AssistConverter assistConverter;
    private final AllergyCheckRule allergyCheckRule;
    private final DosageThresholdService dosageThresholdService;
    private final PrescriptionDraftContext prescriptionDraftContext;
    private final DedupTaskScheduler dedupTaskScheduler;
    private final SuggestionStore suggestionStore;
    private final ObjectMapper objectMapper;
    private final long aiTimeout;
    private final DrugFacade drugFacade;
    private final ExecutorService aiTaskExecutor;

    public PrescriptionAssistServiceImpl(AiService aiService,
                                          AssistConverter assistConverter,
                                          AllergyCheckRule allergyCheckRule,
                                          DosageThresholdService dosageThresholdService,
                                          PrescriptionDraftContext prescriptionDraftContext,
                                          DedupTaskScheduler dedupTaskScheduler,
                                          SuggestionStore suggestionStore,
                                          ObjectMapper objectMapper,
                                          @Value("${ai.timeout.prescription-assist:8}") long aiTimeout,
                                          DrugFacade drugFacade,
                                          @Qualifier("aiTaskExecutor") ExecutorService aiTaskExecutor) {
        this.aiService = aiService;
        this.assistConverter = assistConverter;
        this.allergyCheckRule = allergyCheckRule;
        this.dosageThresholdService = dosageThresholdService;
        this.prescriptionDraftContext = prescriptionDraftContext;
        this.dedupTaskScheduler = dedupTaskScheduler;
        this.suggestionStore = suggestionStore;
        this.objectMapper = objectMapper;
        this.aiTimeout = aiTimeout;
        this.drugFacade = drugFacade;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    @Override
    public PrescriptionAssistResponse assist(PrescriptionAssistRequest request) {
        if (request.getPrescriptionId() == null || request.getPrescriptionId().isBlank()) {
            request.setPrescriptionId(UUID.randomUUID().toString());
        }

        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest aiRequest =
                assistConverter.toAiPrescriptionAssistRequest(request);

        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse aiData = null;
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult;
        try {
            aiResult = aiService.prescriptionAssist(aiRequest).get(aiTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            clearCriticalAlerts(request.getPrescriptionId());
            return buildEmptyResponse(request.getPrescriptionId());
        } catch (ExecutionException e) {
            clearCriticalAlerts(request.getPrescriptionId());
            return buildEmptyResponse(request.getPrescriptionId());
        } catch (TimeoutException e) {
            clearCriticalAlerts(request.getPrescriptionId());
            return buildEmptyResponse(request.getPrescriptionId());
        }

        if (aiResult.isSuccess()) {
            aiData = aiResult.getData();
            String taskId = dedupTaskScheduler.schedule(request.getPrescriptionId());
            scheduleSuggestionAsync(taskId, request);
        }

        if (aiData == null || !aiResult.isSuccess()) {
            clearCriticalAlerts(request.getPrescriptionId());
            return buildEmptyResponse(request.getPrescriptionId());
        }

        boolean hasDrugs = hasDrugsInDraft(aiData.getPrescriptionDraft());
        if (!hasDrugs) {
            PrescriptionAssistResponse response = buildEmptyResponse(request.getPrescriptionId());
            response.setErrorCode(PrescriptionErrorCode.RX_ASSIST_AI_NO_RECOMMENDATION.getCode());
            clearCriticalAlerts(request.getPrescriptionId());
            return response;
        }

        PrescriptionAssistResponse response = assistConverter.toPrescriptionAssistResponse(aiResult);

        List<AllergyWarningItem> allergyWarnings = checkAllergies(request, aiData.getPrescriptionDraft());
        response.setAllergyWarnings(allergyWarnings);

        List<com.aimedical.modules.prescription.context.DosageAlert> criticalContextAlerts = new ArrayList<>();

        List<PrescriptionItem> items = parseDraftItems(aiData.getPrescriptionDraft());

        List<DoseWarning> localDoseWarnings = new ArrayList<>();
        for (PrescriptionItem item : items) {
            DosageCheckRequest doseCheckReq = new DosageCheckRequest();
            doseCheckReq.setDrugCode(item.getDrugId());
            doseCheckReq.setDosage(item.getDose().doubleValue());
            doseCheckReq.setUnit(item.getUnit());
            doseCheckReq.setRouteOfAdministration(item.getRoute());
            doseCheckReq.setFrequency(item.getFrequency());
            if (request.getPatientInfo() != null) {
                doseCheckReq.setPatientAge(request.getPatientInfo().getAge());
            }

            List<com.aimedical.modules.prescription.dto.assist.DosageAlert> itemAlerts =
                    dosageThresholdService.check(doseCheckReq);

            for (com.aimedical.modules.prescription.dto.assist.DosageAlert da : itemAlerts) {
                DoseWarning dw = new DoseWarning();
                dw.setDrugId(da.getDrugCode());
                dw.setWarningType(da.getWarningType());
                dw.setMessage(da.getMessage());
                dw.setSeverity(da.getAlertLevel());
                localDoseWarnings.add(dw);

                if (da.getAlertLevel() == DosageAlertLevel.CRITICAL) {
                    com.aimedical.modules.prescription.context.DosageAlert ctx =
                            new com.aimedical.modules.prescription.context.DosageAlert();
                    ctx.setSeverity(da.getAlertLevel().name());
                    ctx.setMessage(da.getMessage());
                    ctx.setDrugCode(da.getDrugCode());
                    criticalContextAlerts.add(ctx);
                }
            }
        }

        prescriptionDraftContext.updateCriticalAlerts(request.getPrescriptionId(), criticalContextAlerts);

        List<DoseWarning> mergedDoseWarnings = new ArrayList<>();
        if (response.getDoseWarnings() != null) {
            mergedDoseWarnings.addAll(response.getDoseWarnings());
        }
        mergedDoseWarnings.addAll(localDoseWarnings);
        response.setDoseWarnings(mergedDoseWarnings);

        response.setPrescriptionId(request.getPrescriptionId());

        return response;
    }

    @Override
    public DosageCheckResponse checkDose(DosageCheckRequest request) {
        if (request.getPrescriptionId() == null || request.getPrescriptionId().isBlank()) {
            request.setPrescriptionId(UUID.randomUUID().toString());
        }

        List<com.aimedical.modules.prescription.dto.assist.DosageAlert> alerts =
                dosageThresholdService.check(request);

        List<com.aimedical.modules.prescription.context.DosageAlert> criticalAlerts = new ArrayList<>();
        for (com.aimedical.modules.prescription.dto.assist.DosageAlert da : alerts) {
            if (da.getAlertLevel() == DosageAlertLevel.CRITICAL) {
                com.aimedical.modules.prescription.context.DosageAlert ctx =
                        new com.aimedical.modules.prescription.context.DosageAlert();
                ctx.setSeverity(da.getAlertLevel().name());
                ctx.setMessage(da.getMessage());
                ctx.setDrugCode(da.getDrugCode());
                criticalAlerts.add(ctx);
            }
        }
        prescriptionDraftContext.updateCriticalAlerts(request.getPrescriptionId(), criticalAlerts);

        String taskId = dedupTaskScheduler.schedule(request.getPrescriptionId());

        DosageCheckResponse response = new DosageCheckResponse();
        response.setAlerts(alerts);
        response.setTaskId(taskId);
        response.setContextCriticalCount(prescriptionDraftContext.getContextCriticalCount(request.getPrescriptionId()));
        response.setPrescriptionId(request.getPrescriptionId());

        return response;
    }

    @Override
    public AiSuggestionResult getSuggestion(String taskId) {
        Object raw = suggestionStore.get(taskId);
        if (!(raw instanceof AiSuggestionResult result)) {
            throw new BusinessException(PrescriptionErrorCode.RX_ASSIST_SUGGESTION_NOT_FOUND);
        }

        if (result.getStatus() == AiSuggestionStatus.COMPLETED) {
            result.setConsumed(true);
            suggestionStore.put(taskId, result);
        }

        return result;
    }

    private void clearCriticalAlerts(String prescriptionId) {
        prescriptionDraftContext.updateCriticalAlerts(prescriptionId, Collections.emptyList());
    }

    private PrescriptionAssistResponse buildEmptyResponse(String prescriptionId) {
        PrescriptionAssistResponse response = new PrescriptionAssistResponse();
        response.setPrescriptionDraft("{\"drugs\":[]}");
        response.setDoseWarnings(Collections.emptyList());
        response.setAllergyWarnings(Collections.emptyList());
        response.setDisclaimerRequired(true);
        response.setPrescriptionId(prescriptionId);
        response.setErrorCode(PrescriptionErrorCode.RX_ASSIST_AI_NO_RECOMMENDATION.getCode());
        return response;
    }

    private boolean hasDrugsInDraft(String draftJson) {
        try {
            JsonNode root = objectMapper.readTree(draftJson);
            JsonNode drugs = root.get("drugs");
            return drugs != null && drugs.isArray() && drugs.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private List<PrescriptionItem> parseDraftItems(String draftJson) {
        try {
            JsonNode root = objectMapper.readTree(draftJson);
            JsonNode drugs = root.get("drugs");
            if (drugs == null || !drugs.isArray()) return Collections.emptyList();

            List<PrescriptionItem> items = new ArrayList<>();
            for (JsonNode drug : drugs) {
                PrescriptionItem item = new PrescriptionItem();
                JsonNode drugId = drug.get("drugId");
                if (drugId != null) item.setDrugId(drugId.asText());
                JsonNode dose = drug.get("dose");
                if (dose != null) item.setDose(BigDecimal.valueOf(dose.asDouble()));
                JsonNode route = drug.get("route");
                if (route != null) item.setRoute(route.asText());
                JsonNode frequency = drug.get("frequency");
                if (frequency != null) item.setFrequency(frequency.asText());
                JsonNode drugName = drug.get("drugName");
                if (drugName != null) item.setDrugName(drugName.asText());
                JsonNode unit = drug.get("unit");
                if (unit != null) item.setUnit(unit.asText());
                items.add(item);
            }
            return items;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<AllergyWarningItem> checkAllergies(PrescriptionAssistRequest request, String draftJson) {
        if (request.getPatientInfo() == null) {
            return Collections.emptyList();
        }

        List<PrescriptionItem> items = parseDraftItems(draftJson);
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        AuditRequest auditRequest = new AuditRequest();
        auditRequest.setPatientInfo(request.getPatientInfo());
        auditRequest.setPrescriptionItems(items);

        LocalRuleResult ruleResult = allergyCheckRule.check(auditRequest);
        if (ruleResult.isPassed()) {
            return Collections.emptyList();
        }

        AllergyWarningItem warning = new AllergyWarningItem();
        String msg = ruleResult.getMessage();
        if (msg != null) {
            for (PrescriptionItem item : items) {
                if (msg.contains(item.getDrugId())) {
                    warning.setDrugId(item.getDrugId());
                    break;
                }
            }
        }
        if (warning.getDrugId() == null && !items.isEmpty()) {
            warning.setDrugId(items.get(0).getDrugId());
        }

        String[] parts = msg != null ? msg.split(" to ") : new String[0];
        if (parts.length > 1) {
            String allergenPart = parts[1];
            int forIdx = allergenPart.indexOf(" for drug ");
            if (forIdx > 0) {
                warning.setAllergen(allergenPart.substring(0, forIdx));
            } else {
                warning.setAllergen(allergenPart);
            }
        }

        switch (ruleResult.getSeverity()) {
            case BLOCK:
                warning.setSeverity(AllergyWarningSeverity.HIGH);
                break;
            case WARN:
                warning.setSeverity(AllergyWarningSeverity.WARNING);
                break;
            default:
                warning.setSeverity(AllergyWarningSeverity.INFO);
                break;
        }

        return List.of(warning);
    }

    private void scheduleSuggestionAsync(String taskId, PrescriptionAssistRequest request) {
        CompletableFuture.supplyAsync(() -> {
            AiSuggestionResult processingResult = new AiSuggestionResult();
            processingResult.setTaskId(taskId);
            processingResult.setStatus(AiSuggestionStatus.PROCESSING);
            suggestionStore.put(taskId, processingResult);
            AiSuggestionResult result = new AiSuggestionResult();
            result.setTaskId(taskId);
            result.setCreateTime(LocalDateTime.now());
            try {
                com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest aiRequest =
                        assistConverter.toAiPrescriptionAssistRequest(request);
                var aiResult = aiService.prescriptionAssist(aiRequest).get(aiTimeout, TimeUnit.SECONDS);

                if (aiResult.isSuccess() && aiResult.getData() != null) {
                    String suggestion = objectMapper.writeValueAsString(aiResult.getData());
                    result.setSuggestion(suggestion);
                    result.setStatus(AiSuggestionStatus.COMPLETED);
                } else {
                    result.setStatus(AiSuggestionStatus.FAILED);
                    result.setFailReason("AI result not successful or data is null");
                }
            } catch (TimeoutException e) {
                result.setStatus(AiSuggestionStatus.TIMEOUT);
                result.setFailReason(e.getClass().getName() + ": " + (e.getMessage() != null && e.getMessage().length() > 200 ? e.getMessage().substring(0, 200) : e.getMessage()));
            } catch (ExecutionException e) {
                result.setStatus(AiSuggestionStatus.FAILED);
                result.setFailReason(e.getClass().getName() + ": " + (e.getMessage() != null && e.getMessage().length() > 200 ? e.getMessage().substring(0, 200) : e.getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result.setStatus(AiSuggestionStatus.FAILED);
                result.setFailReason(e.getClass().getName() + ": " + (e.getMessage() != null && e.getMessage().length() > 200 ? e.getMessage().substring(0, 200) : e.getMessage()));
            } catch (Exception e) {
                result.setStatus(AiSuggestionStatus.FAILED);
                result.setFailReason(e.getClass().getName() + ": " + (e.getMessage() != null && e.getMessage().length() > 200 ? e.getMessage().substring(0, 200) : e.getMessage()));
            }
            suggestionStore.put(taskId, result);
            return result;
        }, aiTaskExecutor).exceptionally(ex -> {
            log.warn("Async AI suggestion task failed for taskId={}: {}", taskId,
                     ex instanceof CompletionException && ex.getCause() != null
                         ? ex.getCause().getMessage() : ex.getMessage());
            clearCriticalAlerts(request.getPrescriptionId());
            return null;
        });
    }
}
