package com.aimedical.modules.consultation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ObjectMapperJavaTimeModuleEdgeCaseTest {

    private static ObjectMapper createConfiguredMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Test
    void shouldSerializeMidnightToIso8601() throws Exception {
        ObjectMapper mapper = createConfiguredMapper();
        assertEquals("\"2026-06-30T00:00:00\"", mapper.writeValueAsString(LocalDateTime.of(2026, 6, 30, 0, 0, 0)));
    }

    @Test
    void shouldSerializeEndOfMonthToIso8601() throws Exception {
        ObjectMapper mapper = createConfiguredMapper();
        assertEquals("\"2026-01-31T23:59:59\"", mapper.writeValueAsString(LocalDateTime.of(2026, 1, 31, 23, 59, 59)));
    }

    @Test
    void shouldSerializeLastDayOfFebruaryInLeapYear() throws Exception {
        ObjectMapper mapper = createConfiguredMapper();
        assertEquals("\"2024-02-29T12:00:00\"", mapper.writeValueAsString(LocalDateTime.of(2024, 2, 29, 12, 0)));
    }

    @Test
    void shouldNotSerializeLocalDateTimeAsArrayWhenConfiguredCorrectly() throws Exception {
        ObjectMapper mapper = createConfiguredMapper();
        assertEquals("\"2026-06-30T10:00:00\"", mapper.writeValueAsString(LocalDateTime.of(2026, 6, 30, 10, 0, 0)));
    }

    @Test
    void shouldRoundtripSerializationAndDeserialization() throws Exception {
        ObjectMapper mapper = createConfiguredMapper();
        LocalDateTime original = LocalDateTime.of(2026, 6, 30, 10, 0, 0);
        String json = mapper.writeValueAsString(original);
        LocalDateTime restored = mapper.readValue(json, LocalDateTime.class);
        assertEquals(original, restored);
    }

    @Test
    void shouldRoundtripMinLocalDateTime() throws Exception {
        ObjectMapper mapper = createConfiguredMapper();
        LocalDateTime original = LocalDateTime.MIN;
        String json = mapper.writeValueAsString(original);
        LocalDateTime restored = mapper.readValue(json, LocalDateTime.class);
        assertEquals(original, restored);
    }

    @Test
    void shouldRoundtripMaxLocalDateTime() throws Exception {
        ObjectMapper mapper = createConfiguredMapper();
        LocalDateTime original = LocalDateTime.MAX;
        String json = mapper.writeValueAsString(original);
        LocalDateTime restored = mapper.readValue(json, LocalDateTime.class);
        assertEquals(original, restored);
    }

    @Test
    void shouldThrowWhenDeserializingInvalidIso8601() {
        ObjectMapper mapper = createConfiguredMapper();
        assertThrows(Exception.class, () -> mapper.readValue("\"not-a-date\"", LocalDateTime.class));
    }

    @Test
    void shouldReturnNullWhenDeserializingNullToken() throws Exception {
        ObjectMapper mapper = createConfiguredMapper();
        assertNull(mapper.readValue("null", LocalDateTime.class));
    }
}
