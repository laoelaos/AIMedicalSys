package com.aimedical;

import com.aimedical.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void shouldReturnSuccessCode() {
        Result<String> result = controller.ping();
        assertEquals("SUCCESS", result.getCode());
    }

    @Test
    void shouldReturnPongData() {
        Result<String> result = controller.ping();
        assertEquals("pong", result.getData());
    }

    @Test
    void shouldReturnNullMessage() {
        Result<String> result = controller.ping();
        assertNull(result.getMessage());
    }
}
