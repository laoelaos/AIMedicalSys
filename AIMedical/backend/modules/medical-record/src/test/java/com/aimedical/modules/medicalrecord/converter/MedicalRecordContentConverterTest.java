package com.aimedical.modules.medicalrecord.converter;

import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordContentConverterTest {

    private final MedicalRecordContentConverter converter = new MedicalRecordContentConverter();

    private static ch.qos.logback.classic.Logger getLogger() {
        return (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(MedicalRecordContentConverter.class);
    }

    @Test
    void convertToDatabaseColumnShouldReturnNullWhenAttributeIsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumnShouldSerializeMapToJson() {
        Map<MedicalRecordField, String> map = new HashMap<>();
        map.put(MedicalRecordField.CHIEF_COMPLAINT, "头痛三天");
        map.put(MedicalRecordField.PRELIMINARY_DIAGNOSIS, "偏头痛");
        String json = converter.convertToDatabaseColumn(map);
        assertNotNull(json);
        assertTrue(json.contains("CHIEF_COMPLAINT"));
        assertTrue(json.contains("头痛三天"));
        assertTrue(json.contains("PRELIMINARY_DIAGNOSIS"));
        assertTrue(json.contains("偏头痛"));
    }

    @Test
    void convertToDatabaseColumnShouldHandleEmptyMap() {
        String json = converter.convertToDatabaseColumn(Collections.emptyMap());
        assertEquals("{}", json);
    }

    @Test
    void convertToEntityAttributeShouldReturnEmptyMapWhenDbDataIsNull() {
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
    }

    @Test
    void convertToEntityAttributeShouldReturnEmptyMapWhenDbDataIsEmpty() {
        assertTrue(converter.convertToEntityAttribute("").isEmpty());
    }

    @Test
    void convertToEntityAttributeShouldDeserializeJsonToMap() {
        String json = "{\"CHIEF_COMPLAINT\":\"头痛三天\",\"PRELIMINARY_DIAGNOSIS\":\"偏头痛\"}";
        Map<MedicalRecordField, String> map = converter.convertToEntityAttribute(json);
        assertEquals(2, map.size());
        assertEquals("头痛三天", map.get(MedicalRecordField.CHIEF_COMPLAINT));
        assertEquals("偏头痛", map.get(MedicalRecordField.PRELIMINARY_DIAGNOSIS));
    }

    @Test
    void convertToEntityAttributeShouldReturnEmptyMapForInvalidJson() {
        assertTrue(converter.convertToEntityAttribute("{invalid}").isEmpty());
    }

    @Test
    void convertToEntityAttributeShouldReturnEmptyMapForUnknownEnumName() {
        String json = "{\"UNKNOWN_FIELD\":\"value\"}";
        assertTrue(converter.convertToEntityAttribute(json).isEmpty());
    }

    @Test
    void convertToEntityAttributeShouldHandleMixedKnownAndUnknownKeys() {
        String json = "{\"CHIEF_COMPLAINT\":\"头痛\",\"UNKNOWN_FIELD\":\"ignored\"}";
        Map<MedicalRecordField, String> map = converter.convertToEntityAttribute(json);
        assertEquals(0, map.size());
    }

    @Test
    void shouldLogWarnOnDeserializationFailure() {
        ch.qos.logback.classic.Logger logger = getLogger();
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            converter.convertToEntityAttribute("{invalid}");
            assertEquals(1, appender.list.size());
            assertEquals(ch.qos.logback.classic.Level.WARN, appender.list.get(0).getLevel());
            assertTrue(appender.list.get(0).getFormattedMessage().contains("MedicalRecordContentConverter deserialization failed"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldRoundtripCorrectly() {
        Map<MedicalRecordField, String> original = new HashMap<>();
        original.put(MedicalRecordField.CHIEF_COMPLAINT, "头痛");
        original.put(MedicalRecordField.SYMPTOM_DESCRIPTION, "跳痛");
        original.put(MedicalRecordField.PRESENT_ILLNESS, "3天");
        original.put(MedicalRecordField.PAST_HISTORY, "无");
        original.put(MedicalRecordField.PHYSICAL_EXAM, "正常");
        original.put(MedicalRecordField.PRELIMINARY_DIAGNOSIS, "偏头痛");
        original.put(MedicalRecordField.TREATMENT_PLAN, "休息");

        String json = converter.convertToDatabaseColumn(original);
        Map<MedicalRecordField, String> restored = converter.convertToEntityAttribute(json);

        assertEquals(original, restored);
    }
}
