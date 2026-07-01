package com.aimedical.modules.patient.api;

import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.patient.service.TriageRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriageRecordControllerTest {

    @Mock private TriageRecordService triageRecordService;
    @Mock private AuthService authService;

    private TriageRecordController ctrl;

    @BeforeEach
    void setUp() {
        ctrl = new TriageRecordController(triageRecordService, authService);
        CurrentUserResponse user = new CurrentUserResponse();
        user.setUserId(3L);
        when(authService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void listShouldCallService() {
        when(triageRecordService.listByPatient(eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        org.junit.jupiter.api.Assertions.assertNotNull(
                ctrl.list(null, null, null, null, Pageable.unpaged()));
    }

    @Test
    void listDegradedShouldCallService() {
        when(triageRecordService.listDegraded(eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        org.junit.jupiter.api.Assertions.assertNotNull(
                ctrl.list(null, null, null, true, Pageable.unpaged()));
    }
}
