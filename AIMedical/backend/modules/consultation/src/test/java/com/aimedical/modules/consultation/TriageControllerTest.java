package com.aimedical.modules.consultation;

import com.aimedical.common.result.Result;
import com.aimedical.modules.consultation.api.TriageController;
import com.aimedical.modules.consultation.dto.DialogueCreateRequest;
import com.aimedical.modules.consultation.dto.TriageResponse;
import com.aimedical.modules.consultation.service.TriageService;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class TriageControllerTest {

    private final TriageController controller = new TriageController(new StubTriageService());

    @Test
    void shouldDelegateConsultToService() {
        DialogueCreateRequest request = new DialogueCreateRequest();
        request.setChiefComplaint("头痛三天");
        request.setSessionId("session-001");
        Result<TriageResponse> result = controller.consult(request);
        assertEquals("SUCCESS", result.getCode());
        assertNotNull(result.getData());
        assertEquals("session-001", result.getData().getSessionId());
    }

    @Test
    void shouldDelegateSelectDepartmentToServiceWithOverwriteTrue() {
        Result<TriageResponse> result = controller.selectDepartment("session-001", "dept-01", "神经内科");
        assertEquals("SUCCESS", result.getCode());
        assertNotNull(result.getData());
    }

    private static class StubTriageService implements TriageService {
        @Override
        public TriageResponse triage(DialogueCreateRequest request) {
            TriageResponse resp = new TriageResponse();
            resp.setSessionId(request.getSessionId());
            resp.setReason("test");
            resp.setDepartments(Collections.emptyList());
            return resp;
        }

        @Override
        public TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName) {
            TriageResponse resp = new TriageResponse();
            resp.setSessionId(sessionId);
            resp.setReason("Department selected");
            return resp;
        }
    }
}
