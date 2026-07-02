package com.aimedical.modules.patient.api;

import com.aimedical.common.config.GlobalExceptionHandler;
import com.aimedical.common.util.MessageInterpolator;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.patient.dto.CancelResponse;
import com.aimedical.modules.patient.dto.RegistrationResponse;
import com.aimedical.modules.patient.service.PatientRegistrationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PatientRegistrationControllerTest {

    private MockMvc mockMvc;

    @Mock private PatientRegistrationService registrationService;
    @Mock private AuthService authService;
    @Mock private MessageInterpolator messageInterpolator;

    @BeforeEach
    void setUp() {
        lenient().when(messageInterpolator.interpolate(any(), any())).thenReturn("mock error");
        PatientRegistrationController ctrl = new PatientRegistrationController(registrationService, authService);
        mockMvc = MockMvcBuilders.standaloneSetup(ctrl)
                .setControllerAdvice(new GlobalExceptionHandler(messageInterpolator))
                .build();
        CurrentUserResponse user = new CurrentUserResponse();
        user.setUserId(3L);
        when(authService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void listShouldReturn200() throws Exception {
        RegistrationResponse resp = new RegistrationResponse();
        resp.setId(1L);
        resp.setRegistrationType("OUTPATIENT");
        resp.setStatus("PENDING");
        resp.setCanCancel(true);
        when(registrationService.listByUser(3L)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/patient/registration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void cancelShouldReturn200() throws Exception {
        CancelResponse resp = new CancelResponse();
        resp.setSuccess(true);
        resp.setMessage("挂号已成功取消");
        when(registrationService.cancel(1L, 3L)).thenReturn(resp);

        mockMvc.perform(post("/api/patient/registration/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));
    }
}
