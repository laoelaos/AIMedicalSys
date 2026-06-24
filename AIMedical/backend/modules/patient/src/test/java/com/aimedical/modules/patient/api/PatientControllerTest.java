package com.aimedical.modules.patient.api;

import com.aimedical.common.config.GlobalExceptionHandler;
import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.dto.LoginRequest;
import com.aimedical.modules.commonmodule.api.dto.RegisterRequest;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;
import com.aimedical.modules.patient.dto.AllergyRequest;
import com.aimedical.modules.patient.dto.AllergyResponse;
import com.aimedical.modules.patient.service.PatientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PatientService patientService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        PatientController controller = new PatientController(patientService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("register success")
    void registerSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138000");
        req.setPassword("pass1234");
        req.setName("张三");
        req.setGender("男");
        req.setAge(30);

        TokenResponse token = new TokenResponse("access-token", "refresh-token", 7200);
        when(patientService.register(any())).thenReturn(Result.success(token));

        mockMvc.perform(post("/api/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("register invalid phone returns 400")
    void registerInvalidPhone() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setPhone("12345");
        req.setPassword("pass1234");
        req.setName("张三");
        req.setGender("男");
        req.setAge(30);

        mockMvc.perform(post("/api/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login success")
    void loginSuccess() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setPhone("13800138000");
        req.setPassword("pass1234");

        TokenResponse token = new TokenResponse("access-token", "refresh-token", 7200);
        when(patientService.login(any())).thenReturn(Result.success(token));

        mockMvc.perform(post("/api/patient/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    @DisplayName("add allergy success")
    void addAllergySuccess() throws Exception {
        AllergyRequest req = new AllergyRequest();
        req.setAllergen("青霉素");

        AllergyResponse resp = new AllergyResponse();
        resp.setId(1L);
        resp.setAllergen("青霉素");
        when(patientService.addAllergy(any())).thenReturn(Result.success(resp));

        mockMvc.perform(post("/api/patient/health-record/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.allergen").value("青霉素"));
    }
}
