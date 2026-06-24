package com.aimedical.modules.patient.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.PatientErrorCode;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.commonmodule.api.dto.RegisterRequest;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.aimedical.modules.patient.entity.PatientEntity;
import com.aimedical.modules.patient.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock private AuthService authService;
    @Mock private UserRepository userRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private PatientAllergyRepository allergyRepo;
    @Mock private PatientChronicDiseaseRepository chronicRepo;
    @Mock private PatientFamilyHistoryRepository familyRepo;
    @Mock private PatientSurgeryHistoryRepository surgeryRepo;
    @Mock private PatientMedicationHistoryRepository medicationRepo;

    private PatientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PatientServiceImpl(authService, userRepository, patientRepository,
                allergyRepo, chronicRepo, familyRepo, surgeryRepo, medicationRepo);
    }

    @Test
    void shouldRegisterAndCreatePatientProfile() {
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138000");
        req.setPassword("pass1234");
        req.setName("张三");
        req.setGender("男");
        req.setAge(30);

        TokenResponse token = new TokenResponse("at", "rt", 7200);
        User user = new User();
        user.setId(1L);
        user.setUsername("13800138000");

        when(authService.register(any())).thenReturn(token);
        when(userRepository.findByUsername("13800138000")).thenReturn(Optional.of(user));
        when(patientRepository.save(any(PatientEntity.class))).thenAnswer(inv -> {
            PatientEntity p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        var result = service.register(req);
        assertEquals("SUCCESS", result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    void shouldFailLoginWithWrongCredentials() {
        when(authService.getCurrentUser()).thenThrow(new BusinessException(PatientErrorCode.PATIENT_LOGIN_FAILED));
        assertThrows(BusinessException.class, () -> service.getProfile());
    }
}
