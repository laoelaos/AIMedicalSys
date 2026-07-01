package com.aimedical.modules.prescription.event;

import com.aimedical.modules.prescription.cache.DrugDictCacheManager;
import com.aimedical.modules.prescription.repository.DrugAllergyMappingRepository;
import com.aimedical.modules.prescription.repository.DrugCompositionDictRepository;
import com.aimedical.modules.prescription.repository.DrugContraindicationMappingRepository;
import com.aimedical.modules.prescription.rule.entity.DrugAllergyMapping;
import com.aimedical.modules.prescription.rule.entity.DrugCompositionDict;
import com.aimedical.modules.prescription.rule.entity.DrugContraindicationMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class DrugDictChangeEventListenerTest {

    private DrugDictChangeEventListener listener;
    private SpyCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        StubContraindicationRepo ciRepo = new StubContraindicationRepo();
        StubAllergyMappingRepo amRepo = new StubAllergyMappingRepo();
        StubCompositionDictRepo cdRepo = new StubCompositionDictRepo();
        cacheManager = new SpyCacheManager(ciRepo, amRepo, cdRepo);
        listener = new DrugDictChangeEventListener(cacheManager);
    }

    @Test
    void shouldInvalidateContraindicationOnEvent() {
        DrugContraindicationChangeEvent event = new DrugContraindicationChangeEvent(
                DrugDictChangeEvent.ChangeType.UPDATE, "drug-001");

        listener.handleContraindicationChange(event);

        assertEquals(1, cacheManager.contraindicationInvalidateCount);
        assertEquals("drug-001", cacheManager.lastContraindicationKey);
    }

    @Test
    void shouldInvalidateAllergyMappingOnEvent() {
        DrugAllergyMappingChangeEvent event = new DrugAllergyMappingChangeEvent(
                DrugDictChangeEvent.ChangeType.CREATE, "drug-002");

        listener.handleAllergyMappingChange(event);

        assertEquals(1, cacheManager.allergyMappingInvalidateCount);
        assertEquals("drug-002", cacheManager.lastAllergyMappingKey);
    }

    @Test
    void shouldInvalidateCompositionDictOnEvent() {
        DrugCompositionDictChangeEvent event = new DrugCompositionDictChangeEvent(
                DrugDictChangeEvent.ChangeType.DELETE, "drug-003");

        listener.handleCompositionDictChange(event);

        assertEquals(1, cacheManager.compositionDictInvalidateCount);
        assertEquals("drug-003", cacheManager.lastCompositionDictKey);
    }

    private static class SpyCacheManager extends DrugDictCacheManager {
        int contraindicationInvalidateCount = 0;
        int allergyMappingInvalidateCount = 0;
        int compositionDictInvalidateCount = 0;
        String lastContraindicationKey;
        String lastAllergyMappingKey;
        String lastCompositionDictKey;

        SpyCacheManager(DrugContraindicationMappingRepository ciRepo,
                        DrugAllergyMappingRepository amRepo,
                        DrugCompositionDictRepository cdRepo) {
            super(ciRepo, amRepo, cdRepo);
        }

        @Override
        public void invalidateContraindication(String drugCode) {
            contraindicationInvalidateCount++;
            lastContraindicationKey = drugCode;
        }

        @Override
        public void invalidateAllergyMapping(String drugCode) {
            allergyMappingInvalidateCount++;
            lastAllergyMappingKey = drugCode;
        }

        @Override
        public void invalidateCompositionDict(String drugCode) {
            compositionDictInvalidateCount++;
            lastCompositionDictKey = drugCode;
        }
    }

    private static class StubContraindicationRepo implements DrugContraindicationMappingRepository {
        @Override
        public Optional<DrugContraindicationMapping> findByDrugCode(String drugCode) { return Optional.empty(); }
        @Override
        public Optional<DrugContraindicationMapping> findById(Long id) { return Optional.empty(); }
        @Override
        public boolean existsById(Long id) { return false; }
        @Override
        public List<DrugContraindicationMapping> findAll() { return Collections.emptyList(); }
        @Override
        public List<DrugContraindicationMapping> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override
        public long count() { return 0; }
        @Override
        public void deleteById(Long id) {}
        @Override
        public void delete(DrugContraindicationMapping entity) {}
        @Override
        public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override
        public void deleteAll(Iterable<? extends DrugContraindicationMapping> entities) {}
        @Override
        public void deleteAll() {}
        @Override
        public <S extends DrugContraindicationMapping> S save(S entity) { return entity; }
        @Override
        public <S extends DrugContraindicationMapping> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override
        public void flush() {}
        @Override
        public <S extends DrugContraindicationMapping> S saveAndFlush(S entity) { return entity; }
        @Override
        public <S extends DrugContraindicationMapping> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override
        public void deleteAllInBatch(Iterable<DrugContraindicationMapping> entities) {}
        @Override
        public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override
        public void deleteAllInBatch() {}
        @Override
        public DrugContraindicationMapping getOne(Long id) { return null; }
        @Override
        public DrugContraindicationMapping getById(Long id) { return null; }
        @Override
        public DrugContraindicationMapping getReferenceById(Long id) { return null; }
        @Override
        public <S extends DrugContraindicationMapping> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override
        public <S extends DrugContraindicationMapping> List<S> findAll(Example<S> example) { return null; }
        @Override
        public <S extends DrugContraindicationMapping> List<S> findAll(Example<S> example, Sort sort) { return null; }
        @Override
        public <S extends DrugContraindicationMapping> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override
        public <S extends DrugContraindicationMapping> long count(Example<S> example) { return 0; }
        @Override
        public <S extends DrugContraindicationMapping> boolean exists(Example<S> example) { return false; }
        @Override
        public <S extends DrugContraindicationMapping, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override
        public List<DrugContraindicationMapping> findAll(Sort sort) { return Collections.emptyList(); }
        @Override
        public Page<DrugContraindicationMapping> findAll(Pageable pageable) { return null; }
    }

    private static class StubAllergyMappingRepo implements DrugAllergyMappingRepository {
        @Override
        public Optional<DrugAllergyMapping> findByDrugCode(String drugCode) { return Optional.empty(); }
        @Override
        public Optional<DrugAllergyMapping> findById(Long id) { return Optional.empty(); }
        @Override
        public boolean existsById(Long id) { return false; }
        @Override
        public List<DrugAllergyMapping> findAll() { return Collections.emptyList(); }
        @Override
        public List<DrugAllergyMapping> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override
        public long count() { return 0; }
        @Override
        public void deleteById(Long id) {}
        @Override
        public void delete(DrugAllergyMapping entity) {}
        @Override
        public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override
        public void deleteAll(Iterable<? extends DrugAllergyMapping> entities) {}
        @Override
        public void deleteAll() {}
        @Override
        public <S extends DrugAllergyMapping> S save(S entity) { return entity; }
        @Override
        public <S extends DrugAllergyMapping> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override
        public void flush() {}
        @Override
        public <S extends DrugAllergyMapping> S saveAndFlush(S entity) { return entity; }
        @Override
        public <S extends DrugAllergyMapping> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override
        public void deleteAllInBatch(Iterable<DrugAllergyMapping> entities) {}
        @Override
        public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override
        public void deleteAllInBatch() {}
        @Override
        public DrugAllergyMapping getOne(Long id) { return null; }
        @Override
        public DrugAllergyMapping getById(Long id) { return null; }
        @Override
        public DrugAllergyMapping getReferenceById(Long id) { return null; }
        @Override
        public <S extends DrugAllergyMapping> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override
        public <S extends DrugAllergyMapping> List<S> findAll(Example<S> example) { return null; }
        @Override
        public <S extends DrugAllergyMapping> List<S> findAll(Example<S> example, Sort sort) { return null; }
        @Override
        public <S extends DrugAllergyMapping> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override
        public <S extends DrugAllergyMapping> long count(Example<S> example) { return 0; }
        @Override
        public <S extends DrugAllergyMapping> boolean exists(Example<S> example) { return false; }
        @Override
        public <S extends DrugAllergyMapping, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override
        public List<DrugAllergyMapping> findAll(Sort sort) { return Collections.emptyList(); }
        @Override
        public Page<DrugAllergyMapping> findAll(Pageable pageable) { return null; }
    }

    private static class StubCompositionDictRepo implements DrugCompositionDictRepository {
        @Override
        public Optional<DrugCompositionDict> findByDrugCode(String drugCode) { return Optional.empty(); }
        @Override
        public Optional<DrugCompositionDict> findById(Long id) { return Optional.empty(); }
        @Override
        public boolean existsById(Long id) { return false; }
        @Override
        public List<DrugCompositionDict> findAll() { return Collections.emptyList(); }
        @Override
        public List<DrugCompositionDict> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override
        public long count() { return 0; }
        @Override
        public void deleteById(Long id) {}
        @Override
        public void delete(DrugCompositionDict entity) {}
        @Override
        public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override
        public void deleteAll(Iterable<? extends DrugCompositionDict> entities) {}
        @Override
        public void deleteAll() {}
        @Override
        public <S extends DrugCompositionDict> S save(S entity) { return entity; }
        @Override
        public <S extends DrugCompositionDict> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override
        public void flush() {}
        @Override
        public <S extends DrugCompositionDict> S saveAndFlush(S entity) { return entity; }
        @Override
        public <S extends DrugCompositionDict> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override
        public void deleteAllInBatch(Iterable<DrugCompositionDict> entities) {}
        @Override
        public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override
        public void deleteAllInBatch() {}
        @Override
        public DrugCompositionDict getOne(Long id) { return null; }
        @Override
        public DrugCompositionDict getById(Long id) { return null; }
        @Override
        public DrugCompositionDict getReferenceById(Long id) { return null; }
        @Override
        public <S extends DrugCompositionDict> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override
        public <S extends DrugCompositionDict> List<S> findAll(Example<S> example) { return null; }
        @Override
        public <S extends DrugCompositionDict> List<S> findAll(Example<S> example, Sort sort) { return null; }
        @Override
        public <S extends DrugCompositionDict> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override
        public <S extends DrugCompositionDict> long count(Example<S> example) { return 0; }
        @Override
        public <S extends DrugCompositionDict> boolean exists(Example<S> example) { return false; }
        @Override
        public <S extends DrugCompositionDict, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override
        public List<DrugCompositionDict> findAll(Sort sort) { return Collections.emptyList(); }
        @Override
        public Page<DrugCompositionDict> findAll(Pageable pageable) { return null; }
    }
}
