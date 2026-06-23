package com.aimedical.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "phase1"})
class ApplicationContextIT {

    @Test
    void contextLoads() {
    }
}
