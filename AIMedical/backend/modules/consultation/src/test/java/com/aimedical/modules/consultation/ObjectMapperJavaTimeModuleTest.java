package com.aimedical.modules.consultation;

import com.aimedical.modules.commonmodule.event.RegistrationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ObjectMapperJavaTimeModuleTest {

    @Test
    void shouldSerializeLocalDateTimeToIso8601() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalDateTime dateTime = LocalDateTime.of(2026, 6, 30, 10, 0, 0);
        String result = objectMapper.writeValueAsString(dateTime);

        assertEquals("\"2026-06-30T10:00:00\"", result);
    }

    @Test
    void shouldSerializeRegistrationEventWithLocalDateTimeWithoutException() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RegistrationEvent event = new RegistrationEvent(1L, "P001", "session-001",
                "dept-01", "神经内科", 100L, LocalDateTime.of(2026, 6, 30, 10, 0));

        assertDoesNotThrow(() -> {
            String json = objectMapper.writeValueAsString(event);
            assertNotNull(json);
            assertTrue(json.contains("\"registrationId\":1"));
            assertTrue(json.contains("\"eventTime\":\"2026-06-30T10:00:00\""));
        });
    }

    @Test
    void shouldDeserializeIso8601ToLocalDateTime() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalDateTime result = objectMapper.readValue("\"2026-06-30T10:00:00\"", LocalDateTime.class);

        assertEquals(LocalDateTime.of(2026, 6, 30, 10, 0, 0), result);
    }
}
