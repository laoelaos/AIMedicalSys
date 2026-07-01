package com.aimedical.modules.prescription.service.audit.impl;

import com.aimedical.modules.prescription.dto.audit.BlockResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionAuditEnforcerImplTest {

    private final PrescriptionAuditEnforcerImpl enforcer = new PrescriptionAuditEnforcerImpl();

    @Test
    void shouldReturnBlockResponseWithGivenValues() {
        BlockResponse response = enforcer.enforce("rx-001", List.of("Critical dose"), "RX_BLOCK_CRITICAL_DOSE");

        assertEquals(List.of("Critical dose"), response.getBlockReasons());
        assertEquals("RX_BLOCK_CRITICAL_DOSE", response.getBlockCode());
        assertNotNull(response.getBlockTime());
    }
}
