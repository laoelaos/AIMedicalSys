package com.aimedical.modules.medicalrecord.template;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.aimedical.modules.medicalrecord.entity.DeptTemplateConfig;
import com.aimedical.modules.medicalrecord.event.TemplateConfigChangeEvent;
import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import com.aimedical.modules.medicalrecord.repository.DeptTemplateConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

@Component
public class DatabaseTemplateConfigManager implements TemplateConfigManager {

    private static final DepartmentTemplateConfig DEFAULT_TEMPLATE = createDefaultTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DeptTemplateConfigRepository repository;
    private final LoadingCache<String, DepartmentTemplateConfig> templateCache;

    public DatabaseTemplateConfigManager(DeptTemplateConfigRepository repository) {
        this.repository = repository;
        this.templateCache = Caffeine.newBuilder()
                .refreshAfterWrite(60, TimeUnit.SECONDS)
                .build(new CacheLoader<String, DepartmentTemplateConfig>() {
                    @Override
                    public DepartmentTemplateConfig load(String key) {
                        return loadFromDatabase(key);
                    }
                });
    }

    @Override
    public DepartmentTemplateConfig getTemplate(String departmentId) {
        return templateCache.get(departmentId);
    }

    public void refreshTemplate(String departmentId) {
        templateCache.invalidate(departmentId);
    }

    private DepartmentTemplateConfig loadFromDatabase(String departmentId) {
        java.util.Optional<DeptTemplateConfig> opt = repository.findByDepartmentId(departmentId);
        if (opt.isEmpty()) {
            return DEFAULT_TEMPLATE;
        }
        DeptTemplateConfig entity = opt.get();
        try {
            Set<MedicalRecordField> requiredFields = parseRequiredFields(entity.getRequiredFields());
            Map<MedicalRecordField, String> promptMessages = new HashMap<>();
            Map<MedicalRecordField, String> suggestedActions = new HashMap<>();
            if (entity.getTemplateFields() != null && !entity.getTemplateFields().isEmpty()) {
                JsonNode root = objectMapper.readTree(entity.getTemplateFields());
                if (root.has("promptMessages")) {
                    promptMessages.putAll(parseFieldMap(root.get("promptMessages")));
                }
                if (root.has("suggestedActions")) {
                    suggestedActions.putAll(parseFieldMap(root.get("suggestedActions")));
                }
            }
            return new DepartmentTemplateConfig(departmentId, requiredFields, promptMessages, suggestedActions);
        } catch (Exception e) {
            return DEFAULT_TEMPLATE;
        }
    }

    private Set<MedicalRecordField> parseRequiredFields(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptySet();
        }
        try {
            JsonNode arr = objectMapper.readTree(json);
            Set<MedicalRecordField> fields = new java.util.HashSet<>();
            for (JsonNode elem : arr) {
                fields.add(MedicalRecordField.valueOf(elem.asText()));
            }
            return fields;
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private Map<MedicalRecordField, String> parseFieldMap(JsonNode node) {
        Map<MedicalRecordField, String> map = new HashMap<>();
        java.util.Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            map.put(MedicalRecordField.valueOf(entry.getKey()), entry.getValue().asText());
        }
        return map;
    }

    @EventListener
    public void handleTemplateConfigChange(TemplateConfigChangeEvent event) {
        String departmentCode = event.getDepartmentId();
        if (departmentCode != null) {
            templateCache.invalidate(departmentCode);
        } else {
            templateCache.invalidateAll();
        }
    }

    private static DepartmentTemplateConfig createDefaultTemplate() {
        Set<MedicalRecordField> allFields = Arrays.stream(MedicalRecordField.values())
                .filter(f -> f != MedicalRecordField.MISSING_FIELDS && f != MedicalRecordField.PARTIAL_CONTENT)
                .collect(Collectors.toSet());
        Map<MedicalRecordField, String> defaultPrompts = new HashMap<>();
        Map<MedicalRecordField, String> defaultActions = new HashMap<>();
        for (MedicalRecordField field : MedicalRecordField.values()) {
            defaultPrompts.put(field, "{{fieldName}}字段缺失");
            defaultActions.put(field, "请补充{{fieldName}}信息");
        }
        return new DepartmentTemplateConfig("DEFAULT", allFields, defaultPrompts, defaultActions);
    }
}
