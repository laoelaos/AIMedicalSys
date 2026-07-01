package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubmitRequestTest {

    @Test
    void shouldSetAndGetFields() {
        SubmitRequest req = new SubmitRequest();
        req.setPrescriptionId("rx-001");
        req.setPrescriptionItems(List.of(new PrescriptionItem()));
        req.setForceSubmit(true);
        req.setAuditRecordId(100L);

        assertEquals("rx-001", req.getPrescriptionId());
        assertEquals(1, req.getPrescriptionItems().size());
        assertTrue(req.isForceSubmit());
        assertEquals(100L, req.getAuditRecordId().longValue());
    }
}
