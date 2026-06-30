package com.aimedical.modules.patient.api;

import com.aimedical.common.config.GlobalExceptionHandler;
import com.aimedical.common.util.MessageInterpolator;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.patient.dto.TriageRecordResponse;
import com.aimedical.modules.patient.service.TriageRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TriageRecordControllerTest {

    private MockMvc mockMvc;

    @Mock private TriageRecordService triageRecordService;
    @Mock private AuthService authService;
    @Mock private MessageInterpolator messageInterpolator;

    @BeforeEach
    void setUp() {
        lenient().when(messageInterpolator.interpolate(any(), any())).thenReturn("mock error");
        TriageRecordController ctrl = new TriageRecordController(triageRecordService, authService);
        mockMvc = MockMvcBuilders.standaloneSetup(ctrl)
                .setControllerAdvice(new GlobalExceptionHandler(messageInterpolator))
                .build();
        CurrentUserResponse user = new CurrentUserResponse();
        user.setUserId(3L);
        when(authService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void listDegradedShouldReturnFiltered() throws Exception {
        TriageRecordResponse r = new TriageRecordResponse();
        r.setId(2L);
        r.setChiefComplaint("腹痛1天");
        r.setDegraded(true);
        when(triageRecordService.listDegraded(3L)).thenReturn(List.of(r));

        mockMvc.perform(get("/api/patient/triage-records?degraded=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].degraded").value(true));
    }

    @Test
    void listByTimeRangeShouldReturnFiltered() throws Exception {
        TriageRecordResponse r = new TriageRecordResponse();
        r.setId(3L);
        r.setDegraded(false);
        when(triageRecordService.listByTimeRange(eq(3L), anyString(), anyString())).thenReturn(List.of(r));

        mockMvc.perform(get("/api/patient/triage-records?startTime=2026-06-01T00:00:00&endTime=2026-06-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3));
    }
}
