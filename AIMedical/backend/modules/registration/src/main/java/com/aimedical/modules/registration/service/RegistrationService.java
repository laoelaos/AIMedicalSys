package com.aimedical.modules.registration.service;

import com.aimedical.modules.registration.dto.CancelRegistrationRequest;
import com.aimedical.modules.registration.dto.RegistrationDTO;
import com.aimedical.modules.registration.dto.TriageRecordDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RegistrationService {

    RegistrationDTO createRegistration(RegistrationDTO dto);

    RegistrationDTO getRegistration(Long id);

    Page<RegistrationDTO> getRegistrationsByPatient(Long patientId, Pageable pageable);

    Page<RegistrationDTO> getRegistrationsByDoctor(Long doctorId, Pageable pageable);

    RegistrationDTO confirmRegistration(Long id);

    RegistrationDTO completeRegistration(Long id);

    RegistrationDTO cancelRegistration(Long id, CancelRegistrationRequest request);

    RegistrationDTO markNoShow(Long id);

    TriageRecordDTO createTriageRecord(TriageRecordDTO dto);

    TriageRecordDTO getTriageRecord(Long registrationId);
}