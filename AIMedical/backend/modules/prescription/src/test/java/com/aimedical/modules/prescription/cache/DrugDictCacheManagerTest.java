package com.aimedical.modules.prescription.cache;

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
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class DrugDictCacheManagerTest {

    private StubContraindicationRepo contraindicationRepo;
    private StubAllergyMappingRepo allergyMappingRepo;
    private StubCompositionDictRepo compositionDictRepo;
    private DrugDictCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        contraindicationRepo = new StubContraindicationRepo();
        allergyMappingRepo = new StubAllergyMappingRepo();
        compositionDictRepo = new StubCompositionDictRepo();
        cacheManager = new DrugDictCacheManager(contraindicationRepo, allergyMappingRepo, compositionDictRepo);
    }

    @Test
    void shouldLoadContraindicationFromRepo() {
        DrugContraindicationMapping expected = new DrugContraindicationMapping();
        expected.setDrugCode("drug-001");
        contraindicationRepo.entity = expected;

        DrugContraindicationMapping result = cacheManager.getContraindication("drug-001");

        assertNotNull(result);
        assertEquals("drug-001", result.getDrugCode());
    }

    @Test
    void shouldReturnNullWhenContraindicationNotFound() {
        DrugContraindicationMapping result = cacheManager.getContraindication("unknown");
        assertNull(result);
    }

    @Test
    void shouldCacheContraindicationOnSecondCall() {
        DrugContraindicationMapping expected = new DrugContraindicationMapping();
        expected.setDrugCode("drug-001");
        contraindicationRepo.entity = expected;

        DrugContraindicationMapping first = cacheManager.getContraindication("drug-001");
        contraindicationRepo.entity = null;

        DrugContraindicationMapping second = cacheManager.getContraindication("drug-001");
        assertSame(first, second);
    }

    @Test
    void shouldLoadAllergyMappingFromRepo() {
        DrugAllergyMapping expected = new DrugAllergyMapping();
        expected.setDrugCode("drug-001");
        allergyMappingRepo.entity = expected;

        DrugAllergyMapping result = cacheManager.getAllergyMapping("drug-001");

        assertNotNull(result);
        assertEquals("drug-001", result.getDrugCode());
    }

    @Test
    void shouldReturnNullWhenAllergyMappingNotFound() {
        DrugAllergyMapping result = cacheManager.getAllergyMapping("unknown");
        assertNull(result);
    }

    @Test
    void shouldCacheAllergyMappingOnSecondCall() {
        DrugAllergyMapping expected = new DrugAllergyMapping();
        expected.setDrugCode("drug-001");
        allergyMappingRepo.entity = expected;

        DrugAllergyMapping first = cacheManager.getAllergyMapping("drug-001");
        allergyMappingRepo.entity = null;

        DrugAllergyMapping second = cacheManager.getAllergyMapping("drug-001");
        assertSame(first, second);
    }

    @Test
    void shouldLoadCompositionDictFromRepo() {
        DrugCompositionDict expected = new DrugCompositionDict();
        expected.setDrugCode("drug-001");
        compositionDictRepo.entity = expected;

        DrugCompositionDict result = cacheManager.getCompositionDict("drug-001");

        assertNotNull(result);
        assertEquals("drug-001", result.getDrugCode());
    }

    @Test
    void shouldReturnNullWhenCompositionDictNotFound() {
        DrugCompositionDict result = cacheManager.getCompositionDict("unknown");
        assertNull(result);
    }

    @Test
    void shouldCacheCompositionDictOnSecondCall() {
        DrugCompositionDict expected = new DrugCompositionDict();
        expected.setDrugCode("drug-001");
        compositionDictRepo.entity = expected;

        DrugCompositionDict first = cacheManager.getCompositionDict("drug-001");
        compositionDictRepo.entity = null;

        DrugCompositionDict second = cacheManager.getCompositionDict("drug-001");
        assertSame(first, second);
    }

    @Test
    void shouldInvalidateContraindication() {
        DrugContraindicationMapping expected = new DrugContraindicationMapping();
        expected.setDrugCode("drug-001");
        contraindicationRepo.entity = expected;

        DrugContraindicationMapping first = cacheManager.getContraindication("drug-001");
        cacheManager.invalidateContraindication("drug-001");
        contraindicationRepo.entity = null;

        DrugContraindicationMapping second = cacheManager.getContraindication("drug-001");
        assertNull(second);
    }

    @Test
    void shouldInvalidateAllergyMapping() {
        DrugAllergyMapping expected = new DrugAllergyMapping();
        expected.setDrugCode("drug-001");
        allergyMappingRepo.entity = expected;

        DrugAllergyMapping first = cacheManager.getAllergyMapping("drug-001");
        cacheManager.invalidateAllergyMapping("drug-001");
        allergyMappingRepo.entity = null;

        DrugAllergyMapping second = cacheManager.getAllergyMapping("drug-001");
        assertNull(second);
    }

    @Test
    void shouldInvalidateCompositionDict() {
        DrugCompositionDict expected = new DrugCompositionDict();
        expected.setDrugCode("drug-001");
        compositionDictRepo.entity = expected;

        DrugCompositionDict first = cacheManager.getCompositionDict("drug-001");
        cacheManager.invalidateCompositionDict("drug-001");
        compositionDictRepo.entity = null;

        DrugCompositionDict second = cacheManager.getCompositionDict("drug-001");
        assertNull(second);
    }

    @Test
    void shouldInvalidateAllCaches() {
        DrugContraindicationMapping ci = new DrugContraindicationMapping();
        ci.setDrugCode("drug-001");
        contraindicationRepo.entity = ci;

        DrugAllergyMapping am = new DrugAllergyMapping();
        am.setDrugCode("drug-001");
        allergyMappingRepo.entity = am;

        DrugCompositionDict cd = new DrugCompositionDict();
        cd.setDrugCode("drug-001");
        compositionDictRepo.entity = cd;

        cacheManager.getContraindication("drug-001");
        cacheManager.getAllergyMapping("drug-001");
        cacheManager.getCompositionDict("drug-001");

        cacheManager.invalidateAll();

        contraindicationRepo.entity = null;
        allergyMappingRepo.entity = null;
        compositionDictRepo.entity = null;

        assertNull(cacheManager.getContraindication("drug-001"));
        assertNull(cacheManager.getAllergyMapping("drug-001"));
        assertNull(cacheManager.getCompositionDict("drug-001"));
    }

    private static class StubContraindicationRepo implements DrugContraindicationMappingRepository {
        DrugContraindicationMapping entity;

        @Override
        public Optional<DrugContraindicationMapping> findByDrugCode(String drugCode) {
            if (entity != null && drugCode.equals(entity.getDrugCode())) {
                return Optional.of(entity);
            }
            return Optional.empty();
        }

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
        DrugAllergyMapping entity;

        @Override
        public Optional<DrugAllergyMapping> findByDrugCode(String drugCode) {
            if (entity != null && drugCode.equals(entity.getDrugCode())) {
                return Optional.of(entity);
            }
            return Optional.empty();
        }

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
        DrugCompositionDict entity;

        @Override
        public Optional<DrugCompositionDict> findByDrugCode(String drugCode) {
            if (entity != null && drugCode.equals(entity.getDrugCode())) {
                return Optional.of(entity);
            }
            return Optional.empty();
        }

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
