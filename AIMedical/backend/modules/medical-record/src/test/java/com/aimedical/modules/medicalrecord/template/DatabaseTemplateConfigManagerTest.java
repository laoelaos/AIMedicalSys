package com.aimedical.modules.medicalrecord.template;

import com.aimedical.modules.medicalrecord.entity.DeptTemplateConfig;
import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import com.aimedical.modules.medicalrecord.event.TemplateConfigChangeEvent;
import com.aimedical.modules.medicalrecord.repository.DeptTemplateConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseTemplateConfigManagerTest {

    private StubRepository stubRepo;
    private DatabaseTemplateConfigManager manager;

    @BeforeEach
    void setUp() {
        stubRepo = new StubRepository();
        manager = new DatabaseTemplateConfigManager(stubRepo);
    }

    @Test
    void shouldReturnDefaultTemplateWhenDepartmentNotFound() {
        DepartmentTemplateConfig config = manager.getTemplate("unknown-dept");
        assertEquals("DEFAULT", config.getDepartmentId());
        assertEquals(7, config.getRequiredFields().size());
    }

    @Test
    void shouldLoadFromDatabaseAndReturnConfig() {
        stubRepo.entity = createEntity("dept-01",
                "[\"CHIEF_COMPLAINT\",\"SYMPTOM_DESCRIPTION\"]",
                "{\"promptMessages\":{\"CHIEF_COMPLAINT\":\"请描述主诉\"},\"suggestedActions\":{\"CHIEF_COMPLAINT\":\"询问患者\"}}");

        DepartmentTemplateConfig config = manager.getTemplate("dept-01");

        assertEquals("dept-01", config.getDepartmentId());
        assertEquals(Set.of(MedicalRecordField.CHIEF_COMPLAINT, MedicalRecordField.SYMPTOM_DESCRIPTION),
                config.getRequiredFields());
        assertEquals("请描述主诉", config.getPromptMessages().get(MedicalRecordField.CHIEF_COMPLAINT));
        assertEquals("询问患者", config.getSuggestedActions().get(MedicalRecordField.CHIEF_COMPLAINT));
    }

    @Test
    void shouldReturnDefaultTemplateOnParseErrorInRequiredFields() {
        stubRepo.entity = createEntity("dept-01", "invalid json", null);
        DepartmentTemplateConfig config = manager.getTemplate("dept-01");
        assertEquals("dept-01", config.getDepartmentId());
    }

    @Test
    void shouldReturnDefaultTemplateOnParseErrorInTemplateFields() {
        stubRepo.entity = createEntity("dept-01", "[\"CHIEF_COMPLAINT\"]", "invalid json");
        DepartmentTemplateConfig config = manager.getTemplate("dept-01");
        assertEquals("DEFAULT", config.getDepartmentId());
    }

    @Test
    void shouldReturnDefaultTemplateWhenEnumNameInvalid() {
        stubRepo.entity = createEntity("dept-01",
                "[\"INVALID_FIELD\"]",
                "{\"promptMessages\":{},\"suggestedActions\":{}}");
        DepartmentTemplateConfig config = manager.getTemplate("dept-01");
        assertEquals("dept-01", config.getDepartmentId());
    }

    @Test
    void shouldReturnDefaultTemplateWhenNullRequiredFields() {
        stubRepo.entity = createEntity("dept-01", null, null);
        DepartmentTemplateConfig config = manager.getTemplate("dept-01");
        assertEquals("dept-01", config.getDepartmentId());
    }

    @Test
    void shouldHandleMissingPromptMessages() {
        stubRepo.entity = createEntity("dept-01",
                "[\"CHIEF_COMPLAINT\"]",
                "{\"suggestedActions\":{\"CHIEF_COMPLAINT\":\"询问患者\"}}");

        DepartmentTemplateConfig config = manager.getTemplate("dept-01");
        assertEquals("dept-01", config.getDepartmentId());
        assertTrue(config.getPromptMessages().isEmpty());
        assertEquals("询问患者", config.getSuggestedActions().get(MedicalRecordField.CHIEF_COMPLAINT));
    }

    @Test
    void shouldHandleMissingSuggestedActions() {
        stubRepo.entity = createEntity("dept-01",
                "[\"CHIEF_COMPLAINT\"]",
                "{\"promptMessages\":{\"CHIEF_COMPLAINT\":\"请描述主诉\"}}");

        DepartmentTemplateConfig config = manager.getTemplate("dept-01");
        assertTrue(config.getSuggestedActions().isEmpty());
        assertEquals("请描述主诉", config.getPromptMessages().get(MedicalRecordField.CHIEF_COMPLAINT));
    }

    @Test
    void shouldReturnCachedValueOnSecondCall() {
        stubRepo.entity = createEntity("dept-01",
                "[\"CHIEF_COMPLAINT\"]",
                "{\"promptMessages\":{},\"suggestedActions\":{}}");

        DepartmentTemplateConfig first = manager.getTemplate("dept-01");
        stubRepo.entity = null;

        DepartmentTemplateConfig second = manager.getTemplate("dept-01");
        assertSame(first, second);
    }

    @Test
    void shouldLoadFreshAfterRefresh() {
        stubRepo.entity = createEntity("dept-01",
                "[\"CHIEF_COMPLAINT\"]",
                "{\"promptMessages\":{},\"suggestedActions\":{}}");
        DepartmentTemplateConfig first = manager.getTemplate("dept-01");

        stubRepo.entity = createEntity("dept-01",
                "[\"CHIEF_COMPLAINT\",\"SYMPTOM_DESCRIPTION\"]",
                "{\"promptMessages\":{},\"suggestedActions\":{}}");
        manager.refreshTemplate("dept-01");

        DepartmentTemplateConfig second = manager.getTemplate("dept-01");
        assertNotSame(first, second);
        assertEquals(2, second.getRequiredFields().size());
    }

    @Test
    void shouldInvalidateCacheOnTemplateConfigChangeEvent() {
        stubRepo.entity = createEntity("dept-01",
                "[\"CHIEF_COMPLAINT\"]",
                "{\"promptMessages\":{},\"suggestedActions\":{}}");
        DepartmentTemplateConfig first = manager.getTemplate("dept-01");

        stubRepo.entity = createEntity("dept-01",
                "[\"CHIEF_COMPLAINT\",\"SYMPTOM_DESCRIPTION\"]",
                "{\"promptMessages\":{},\"suggestedActions\":{}}");

        manager.handleTemplateConfigChange(new TemplateConfigChangeEvent("dept-01"));

        DepartmentTemplateConfig second = manager.getTemplate("dept-01");
        assertNotSame(first, second);
        assertEquals(2, second.getRequiredFields().size());
    }

    @Test
    void shouldInvalidateCacheOnTemplateConfigChangeEventWithNullDepartmentId() {
        stubRepo.entity = createEntity("dept-01",
                "[\"CHIEF_COMPLAINT\"]",
                "{\"promptMessages\":{},\"suggestedActions\":{}}");
        DepartmentTemplateConfig first = manager.getTemplate("dept-01");

        stubRepo.entity = createEntity("dept-01",
                "[\"CHIEF_COMPLAINT\",\"SYMPTOM_DESCRIPTION\"]",
                "{\"promptMessages\":{},\"suggestedActions\":{}}");

        manager.handleTemplateConfigChange(new TemplateConfigChangeEvent(null));

        DepartmentTemplateConfig second = manager.getTemplate("dept-01");
        assertNotSame(first, second);
        assertEquals(2, second.getRequiredFields().size());
    }

    @Test
    void defaultTemplateShouldHaveAllSevenFieldsWithPlaceholders() {
        DepartmentTemplateConfig def = manager.getTemplate("non-existent");
        assertEquals(7, def.getRequiredFields().size());
        assertEquals("{{fieldName}}字段缺失", def.getPromptMessages().get(MedicalRecordField.CHIEF_COMPLAINT));
        assertEquals("请补充{{fieldName}}信息", def.getSuggestedActions().get(MedicalRecordField.CHIEF_COMPLAINT));
    }

    private static DeptTemplateConfig createEntity(String deptId, String requiredFields, String templateFields) {
        DeptTemplateConfig entity = new DeptTemplateConfig();
        entity.setDepartmentId(deptId);
        entity.setRequiredFields(requiredFields);
        entity.setTemplateFields(templateFields);
        return entity;
    }

    private static class StubRepository implements DeptTemplateConfigRepository {
        DeptTemplateConfig entity;

        @Override
        public Optional<DeptTemplateConfig> findByDepartmentId(String departmentId) {
            if (entity != null && departmentId.equals(entity.getDepartmentId())) {
                return Optional.of(entity);
            }
            return Optional.empty();
        }

        @Override
        public DeptTemplateConfig save(DeptTemplateConfig entity) { return entity; }
        @Override
        public Optional<DeptTemplateConfig> findById(Long id) { return Optional.empty(); }
        @Override
        public boolean existsById(Long id) { return false; }
        @Override
        public java.util.List<DeptTemplateConfig> findAll() { return Collections.emptyList(); }
        @Override
        public java.util.List<DeptTemplateConfig> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override
        public long count() { return 0; }
        @Override
        public void deleteById(Long id) {}
        @Override
        public void delete(DeptTemplateConfig entity) {}
        @Override
        public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override
        public void deleteAll(Iterable<? extends DeptTemplateConfig> entities) {}
        @Override
        public void deleteAll() {}
        @Override
        public void flush() {}
        @Override
        public <S extends DeptTemplateConfig> S saveAndFlush(S entity) { return entity; }
        @Override
        public <S extends DeptTemplateConfig> java.util.List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override
        public void deleteAllInBatch(Iterable<DeptTemplateConfig> entities) {}
        @Override
        public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override
        public void deleteAllInBatch() {}
        @Override
        public DeptTemplateConfig getOne(Long id) { return null; }
        @Override
        public DeptTemplateConfig getById(Long id) { return null; }
        @Override
        public DeptTemplateConfig getReferenceById(Long id) { return null; }
        @Override
        public <S extends DeptTemplateConfig> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override
        public <S extends DeptTemplateConfig> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example) { return null; }
        @Override
        public <S extends DeptTemplateConfig> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return null; }
        @Override
        public <S extends DeptTemplateConfig> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override
        public <S extends DeptTemplateConfig> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override
        public <S extends DeptTemplateConfig> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override
        public <S extends DeptTemplateConfig, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override
        public java.util.List<DeptTemplateConfig> findAll(org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override
        public org.springframework.data.domain.Page<DeptTemplateConfig> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
        @Override
        public <S extends DeptTemplateConfig> java.util.List<S> saveAll(Iterable<S> entities) { return null; }
    }
}
