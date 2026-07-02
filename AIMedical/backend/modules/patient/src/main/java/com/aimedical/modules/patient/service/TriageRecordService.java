package com.aimedical.modules.patient.service;

import com.aimedical.modules.patient.dto.TriageRecordRequest;
import com.aimedical.modules.patient.dto.TriageRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface TriageRecordService {

    void saveAsync(TriageRecordRequest request);

    Page<TriageRecordResponse> listByPatient(Long patientId, Pageable pageable);

    Page<TriageRecordResponse> listByTimeRange(Long patientId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    Page<TriageRecordResponse> listDegraded(Long patientId, Pageable pageable);
}
