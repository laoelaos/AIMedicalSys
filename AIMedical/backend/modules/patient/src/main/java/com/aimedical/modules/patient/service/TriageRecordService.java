package com.aimedical.modules.patient.service;

import com.aimedical.modules.patient.dto.TriageRecordRequest;
import com.aimedical.modules.patient.dto.TriageRecordResponse;

import java.util.List;

public interface TriageRecordService {

    void saveAsync(TriageRecordRequest request);

    List<TriageRecordResponse> listByPatient(Long patientId);

    List<TriageRecordResponse> listByTimeRange(Long patientId, String startTime, String endTime);

    List<TriageRecordResponse> listDegraded(Long patientId);
}
