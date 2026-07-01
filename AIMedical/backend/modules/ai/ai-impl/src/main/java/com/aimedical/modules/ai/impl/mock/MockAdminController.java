package com.aimedical.modules.ai.impl.mock;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai/mock")
public class MockAdminController {

    private final MockAiService mockAiService;

    public MockAdminController(MockAiService mockAiService) {
        this.mockAiService = mockAiService;
    }

    @GetMapping("/strategy")
    public ResponseEntity<Map<String, String>> getCurrentStrategy() {
        return ResponseEntity.ok(Map.of("strategy", mockAiService.getStrategy().name()));
    }

    @PostMapping("/strategy")
    public ResponseEntity<Void> setStrategy(@RequestBody MockStrategyRequest request) {
        mockAiService.setStrategy(MockAiService.ResponseStrategy.valueOf(request.getStrategy()));
        return ResponseEntity.ok().build();
    }

    static class MockStrategyRequest {
        private String strategy;

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
    }
}
