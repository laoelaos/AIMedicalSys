package com.aimedical.modules.commonmodule.visit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VisitFacadeTest {

    @Test
    void shouldReturnVisitIdByEncounterId() {
        VisitFacade facade = encounterId -> "VISIT-" + encounterId;
        String result = facade.findVisitIdByEncounterId("ENC001");
        assertEquals("VISIT-ENC001", result);
    }

    @Test
    void shouldReturnNullWhenVisitNotFound() {
        VisitFacade facade = encounterId -> null;
        assertNull(facade.findVisitIdByEncounterId("UNKNOWN"));
    }
}
