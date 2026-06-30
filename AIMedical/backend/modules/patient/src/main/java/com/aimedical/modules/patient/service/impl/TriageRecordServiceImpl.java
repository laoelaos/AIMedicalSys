package com.aimedical.modules.patient.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.patient.dto.TriageRecordRequest;
import com.aimedical.modules.patient.dto.TriageRecordResponse;
import com.aimedical.modules.patient.entity.TriageRecordEntity;
import com.aimedical.modules.patient.repository.TriageRecordRepository;
import com.aimedical.modules.patient.service.TriageRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TriageRecordServiceImpl implements TriageRecordService {

    private static final Logger log = LoggerFactory.getLogger(TriageRecordServiceImpl.class);

    private final TriageRecordRepository triageRecordRepository;

    public TriageRecordServiceImpl(TriageRecordRepository triageRecordRepository) {
        this.triageRecordRepository = triageRecordRepository;
    }

    @Override
    @Async
    @Transactional
    public void saveAsync(TriageRecordRequest request) {
        try {
            TriageRecordEntity entity = new TriageRecordEntity();
            entity.setPatientId(request.getPatientId());
            entity.setChiefComplaint(request.getChiefComplaint());
            entity.setSessionId(request.getSessionId());
            entity.setIsDegraded(request.isDegraded());
            entity.setRuleVersion(request.getRuleVersion());
            entity.setRuleSetId(request.getRuleSetId());

            if (request.getRecommendedDepartments() != null) {
                entity.setRecommendedDepartments(String.join(",", request.getRecommendedDepartments()));
            }
            if (request.getRecommendedDoctors() != null) {
                entity.setRecommendedDoctors(String.join(",", request.getRecommendedDoctors()));
            }
            if (request.getMatchedRules() != null) {
                entity.setMatchedRules(String.join(",", request.getMatchedRules()));
            }

            triageRecordRepository.save(entity);
            log.info("Triage record saved async: patientId={}, sessionId={}",
                    request.getPatientId(), request.getSessionId());
        } catch (Exception e) {
            log.error("Failed to save triage record async: patientId={}", request.getPatientId(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriageRecordResponse> listByPatient(Long patientId) {
        return triageRecordRepository.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(patientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriageRecordResponse> listByTimeRange(Long patientId, String startTime, String endTime) {
        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            end = LocalDateTime.parse(endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new BusinessException(GlobalErrorCode.PARAM_INVALID,
                    "时间格式错误，请使用 ISO 格式：yyyy-MM-ddTHH:mm:ss");
        }
        return triageRecordRepository.findByPatientIdAndTimeRange(patientId, start, end)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriageRecordResponse> listDegraded(Long patientId) {
        return triageRecordRepository.findByPatientIdAndIsDegradedTrueAndDeletedFalseOrderByCreatedAtDesc(patientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private TriageRecordResponse toResponse(TriageRecordEntity e) {
        TriageRecordResponse r = new TriageRecordResponse();
        r.setId(e.getId());
        r.setPatientId(e.getPatientId());
        r.setChiefComplaint(e.getChiefComplaint());
        r.setSessionId(e.getSessionId());
        r.setRecommendedDepartments(e.getRecommendedDepartments());
        r.setRecommendedDoctors(e.getRecommendedDoctors());
        r.setDegraded(e.getIsDegraded());
        r.setRuleVersion(e.getRuleVersion());
        r.setRuleSetId(e.getRuleSetId());
        r.setMatchedRules(e.getMatchedRules());
        r.setCreatedAt(e.getCreatedAt() != null
                ? e.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "");
        return r;
    }
}
