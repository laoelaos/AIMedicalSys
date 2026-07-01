package com.aimedical.modules.prescription.dto.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubmitResponseTest {

    @Test
    void shouldSetAndGetFields() {
        SubmitResponse resp = new SubmitResponse();
        resp.setSubmitted(true);
        resp.setPrescriptionOrderId("order-001");
        resp.setBlockInfo(new BlockResponse());
        resp.setErrorCode("RX_ERR");

        assertTrue(resp.isSubmitted());
        assertEquals("order-001", resp.getPrescriptionOrderId());
        assertNotNull(resp.getBlockInfo());
        assertEquals("RX_ERR", resp.getErrorCode());
    }

    @Test
    void shouldSetAndGetWarnResult() {
        SubmitResponse resp = new SubmitResponse();
        WarnResult warnResult = new WarnResult();
        warnResult.setRiskLevel(com.aimedical.modules.prescription.service.audit.AuditRiskLevel.WARN);
        resp.setWarnResult(warnResult);

        assertNotNull(resp.getWarnResult());
        assertEquals(com.aimedical.modules.prescription.service.audit.AuditRiskLevel.WARN, resp.getWarnResult().getRiskLevel());
    }

    @Test
    void shouldHandleNullWarnResult() {
        SubmitResponse resp = new SubmitResponse();
        assertNull(resp.getWarnResult());
    }
}
