package com.aimedical.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JacksonConfigTest {

    private final JacksonConfig config = new JacksonConfig();

    @Test
    void shouldReturnNonNullCustomizer() {
        Jackson2ObjectMapperBuilderCustomizer customizer = config.customizer();
        assertNotNull(customizer);
    }

    @Test
    void shouldConfigureSnakeCaseNaming() throws Exception {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        config.customizer().customize(builder);
        ObjectMapper mapper = builder.build();

        TestDto dto = new TestDto();
        dto.setUserName("testUser");
        dto.setPhoneNumber("123456");

        String json = mapper.writeValueAsString(dto);
        assertTrue(json.contains("user_name"));
        assertTrue(json.contains("phone_number"));
        assertFalse(json.contains("userName"));
        assertFalse(json.contains("phoneNumber"));
    }

    @Test
    void shouldRegisterJavaTimeModule() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        config.customizer().customize(builder);
        ObjectMapper mapper = builder.build();

        assertFalse(mapper.getRegisteredModuleIds().isEmpty());
    }

    @Test
    void shouldSerializeLocalDateTime() throws Exception {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        config.customizer().customize(builder);
        ObjectMapper mapper = builder.build();

        LocalDateTime now = LocalDateTime.of(2026, 6, 17, 10, 30, 0);
        String json = mapper.writeValueAsString(now);
        assertNotNull(json);
        assertTrue(json.contains("2026"));
    }

    static class TestDto {
        private String userName;
        private String phoneNumber;

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }
}
