package com.aimedical.modules.medicalrecord.converter;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MedicalRecordContentConverter implements AttributeConverter<Map<MedicalRecordField, String>, String> {

    private static final Logger log = LoggerFactory.getLogger(MedicalRecordContentConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<MedicalRecordField, String> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            Map<String, String> plain = attribute.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
            return objectMapper.writeValueAsString(plain);
        } catch (Exception e) {
            log.warn("MedicalRecordContentConverter serialization failed", e);
            return null;
        }
    }

    @Override
    public Map<MedicalRecordField, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, String> plain = objectMapper.readValue(dbData, new TypeReference<Map<String, String>>() {});
            return plain.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> MedicalRecordField.valueOf(e.getKey()),
                            Map.Entry::getValue));
        } catch (Exception e) {
            log.warn("MedicalRecordContentConverter deserialization failed", e);
            return Collections.emptyMap();
        }
    }
}
