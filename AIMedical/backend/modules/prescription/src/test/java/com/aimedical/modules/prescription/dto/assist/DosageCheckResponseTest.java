package com.aimedical.modules.prescription.dto.assist;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DosageCheckResponseTest {

    @Test
    void shouldSetAndGetFields() {
        DosageCheckResponse resp = new DosageCheckResponse();
        resp.setAlerts(List.of(new DosageAlert()));
        resp.setTaskId("task-001");
        resp.setContextCriticalCount(2);
        resp.setPrescriptionId("rx-001");

        assertEquals(1, resp.getAlerts().size());
        assertEquals("task-001", resp.getTaskId());
        assertEquals(2, resp.getContextCriticalCount());
        assertEquals("rx-001", resp.getPrescriptionId());
    }
}
