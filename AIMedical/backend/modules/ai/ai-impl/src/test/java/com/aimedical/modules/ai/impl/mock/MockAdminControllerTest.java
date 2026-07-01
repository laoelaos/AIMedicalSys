package com.aimedical.modules.ai.impl.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockAdminControllerTest {

    private MockAiService mockAiService;
    private MockAdminController controller;

    @BeforeEach
    void setUp() {
        mockAiService = new MockAiService("STATIC");
        controller = new MockAdminController(mockAiService);
    }

    @Test
    void getStrategyShouldReturnStaticByDefault() {
        var response = controller.getCurrentStrategy();
        assertEquals(200, response.getStatusCode().value());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("STATIC", body.get("strategy"));
    }

    @Test
    void setStrategyAndVerify() {
        MockAdminController.MockStrategyRequest request = new MockAdminController.MockStrategyRequest();
        request.setStrategy("AI_UNAVAILABLE");
        var postResponse = controller.setStrategy(request);
        assertEquals(200, postResponse.getStatusCode().value());

        var getResponse = controller.getCurrentStrategy();
        Map<String, String> body = getResponse.getBody();
        assertNotNull(body);
        assertEquals("AI_UNAVAILABLE", body.get("strategy"));
    }

    @Test
    void setStrategyToTimeout() {
        MockAdminController.MockStrategyRequest request = new MockAdminController.MockStrategyRequest();
        request.setStrategy("TIMEOUT");
        controller.setStrategy(request);

        var getResponse = controller.getCurrentStrategy();
        Map<String, String> body = getResponse.getBody();
        assertNotNull(body);
        assertEquals("TIMEOUT", body.get("strategy"));
    }
}
