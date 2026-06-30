package com.aimedical.modules.patient.service;

import com.aimedical.modules.patient.dto.CancelResponse;
import com.aimedical.modules.patient.dto.RegistrationRequest;
import com.aimedical.modules.patient.dto.RegistrationResponse;

import java.util.List;

public interface PatientRegistrationService {

    RegistrationResponse create(RegistrationRequest req, Long userId);

    List<RegistrationResponse> listByUser(Long userId);

    CancelResponse cancel(Long regId, Long userId);
}
