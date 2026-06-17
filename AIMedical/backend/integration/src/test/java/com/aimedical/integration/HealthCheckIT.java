package com.aimedical.integration;

import com.aimedical.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthCheckIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldPingSuccess() {
        ResponseEntity<Result> response = restTemplate.getForEntity("/api/ping", Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getCode());
        assertEquals("pong", response.getBody().getData());
    }
}
