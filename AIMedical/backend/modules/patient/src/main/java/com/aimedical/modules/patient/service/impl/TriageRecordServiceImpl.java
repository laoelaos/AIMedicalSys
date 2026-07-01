package com.aimedical.modules.patient.service.impl;

import com.aimedical.modules.patient.dto.TriageRecordRequest;
import com.aimedical.modules.patient.dto.TriageRecordResponse;
import com.aimedical.modules.patient.entity.TriageRecordEntity;
import com.aimedical.modules.patient.repository.PatientTriageRecordRepository;
import com.aimedical.modules.patient.service.TriageRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TriageRecordServiceImpl implements TriageRecordService {

    private static final Logger log = LoggerFactory.getLogger(TriageRecordServiceImpl.class);

    private final PatientTriageRecordRepository triageRecordRepository;

    public TriageRecordServiceImpl(PatientTriageRecordRepository triageRecordRepository) {
        this.triageRecordRepository = triageRecordRepository;
    }

    @Override
    @Async
    public void saveAsync(TriageRecordRequest request) {
        doSave(request);
    }

    @Transactional
    void doSave(TriageRecordRequest request) {
        TriageRecordEntity entity = new TriageRecordEntity();
        entity.setPatientId(request.getPatientId());
        entity.setChiefComplaint(request.getChiefComplaint());
        entity.setSessionId(request.getSessionId());
        entity.setIsDegraded(request.getIsDegraded());
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
        log.info("Triage record saved: patientId={}, sessionId={}",
                request.getPatientId(), request.getSessionId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TriageRecordResponse> listByPatient(Long patientId, Pageable pageable) {
        return triageRecordRepository.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(patientId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TriageRecordResponse> listByTimeRange(Long patientId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return triageRecordRepository.findByPatientIdAndTimeRange(patientId, startTime, endTime, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TriageRecordResponse> listDegraded(Long patientId, Pageable pageable) {
        return triageRecordRepository.findByPatientIdAndIsDegradedTrueAndDeletedFalseOrderByCreatedAtDesc(patientId, pageable)
                .map(this::toResponse);
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
