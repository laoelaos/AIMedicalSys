package com.aimedical;

import com.aimedical.common.result.Result;
import com.aimedical.HealthController;
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
    void shouldReturnSuccessMessage() {
        Result<String> result = controller.ping();
        assertEquals("成功", result.getMessage());
    }
}
