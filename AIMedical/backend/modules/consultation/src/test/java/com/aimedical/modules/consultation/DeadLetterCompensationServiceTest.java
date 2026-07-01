package com.aimedical.modules.consultation;

import com.aimedical.modules.consultation.entity.DeadLetterEvent;
import com.aimedical.modules.consultation.repository.DeadLetterEventRepository;
import com.aimedical.modules.consultation.service.DeadLetterCompensationService;
import com.aimedical.modules.consultation.service.TriageService;
import com.aimedical.modules.consultation.dto.DialogueCreateRequest;
import com.aimedical.modules.consultation.dto.TriageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class DeadLetterCompensationServiceTest {

    private StubDeadLetterEventRepository eventRepository;
    private StubTriageService triageService;
    private ObjectMapper objectMapper;
    private DeadLetterCompensationService service;

    @BeforeEach
    void setUp() {
        eventRepository = new StubDeadLetterEventRepository();
        triageService = new StubTriageService();
        objectMapper = new ObjectMapper();
        service = new DeadLetterCompensationService(eventRepository, triageService, objectMapper);
    }

    @Test
    void shouldCompensateDeadLetterEvents() throws Exception {
        DeadLetterEvent event = new DeadLetterEvent();
        event.setEventPayload("{\"sessionId\":\"s1\",\"departmentId\":\"dept-01\",\"departmentName\":\"内科\"}");
        eventRepository.events.add(event);

        service.compensateDeadLetters();

        assertEquals("COMPENSATED", event.getState());
        assertTrue(triageService.selectDepartmentCalled);
    }

    @Test
    void shouldIncrementRetryCountOnFailure() throws Exception {
        DeadLetterEvent event = new DeadLetterEvent();
        event.setEventPayload("{\"sessionId\":\"s1\",\"departmentId\":\"dept-01\",\"departmentName\":\"内科\"}");
        event.setRetryCount(1);
        eventRepository.events.add(event);
        triageService.throwException = true;

        service.compensateDeadLetters();

        assertEquals(2, event.getRetryCount().intValue());
        assertEquals("FAILED", event.getState());
    }

    @Test
    void shouldHandleMultipleEvents() throws Exception {
        DeadLetterEvent e1 = new DeadLetterEvent();
        e1.setEventPayload("{\"sessionId\":\"s1\",\"departmentId\":\"dept-01\",\"departmentName\":\"内科\"}");
        DeadLetterEvent e2 = new DeadLetterEvent();
        e2.setEventPayload("{\"sessionId\":\"s2\",\"departmentId\":\"dept-02\",\"departmentName\":\"外科\"}");
        eventRepository.events.add(e1);
        eventRepository.events.add(e2);

        service.compensateDeadLetters();

        assertEquals("COMPENSATED", e1.getState());
        assertEquals("COMPENSATED", e2.getState());
    }

    @Test
    void shouldExpireWhenRetryCountExceedsMaxOnPreCheck() throws Exception {
        DeadLetterEvent event = new DeadLetterEvent();
        event.setEventPayload("{\"sessionId\":\"s1\",\"departmentId\":\"dept-01\",\"departmentName\":\"内科\"}");
        event.setRetryCount(3);
        event.setMaxRetryCount(3);
        eventRepository.events.add(event);

        service.compensateDeadLetters();

        assertEquals("EXPIRED", event.getState());
        assertEquals(3, event.getRetryCount().intValue());
        assertFalse(triageService.selectDepartmentCalled);
    }

    @Test
    void shouldExpireWhenRetryCountExceedsMaxOnCatch() throws Exception {
        DeadLetterEvent event = new DeadLetterEvent();
        event.setEventPayload("{\"sessionId\":\"s1\",\"departmentId\":\"dept-01\",\"departmentName\":\"内科\"}");
        event.setRetryCount(2);
        event.setMaxRetryCount(3);
        eventRepository.events.add(event);
        triageService.throwException = true;

        service.compensateDeadLetters();

        assertEquals(3, event.getRetryCount().intValue());
        assertEquals("EXPIRED", event.getState());
    }

    @Test
    void shouldSkipWhenNoEvents() {
        service.compensateDeadLetters();
        assertFalse(triageService.selectDepartmentCalled);
    }

    private static class StubDeadLetterEventRepository implements DeadLetterEventRepository {
        List<DeadLetterEvent> events = new ArrayList<>();

        @Override
        public List<DeadLetterEvent> findByCompensableEvents(String state) {
            return events;
        }

        @Override
        public DeadLetterEvent save(DeadLetterEvent entity) {
            return entity;
        }

        @Override public List<DeadLetterEvent> findAll() { return events; }
        @Override public List<DeadLetterEvent> findAll(Sort sort) { return events; }
        @Override public List<DeadLetterEvent> findAllById(Iterable<Long> longs) { return events; }
        @Override public <S extends DeadLetterEvent> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public void flush() {}
        @Override public <S extends DeadLetterEvent> S saveAndFlush(S entity) { return entity; }
        @Override public <S extends DeadLetterEvent> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<DeadLetterEvent> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public DeadLetterEvent getOne(Long aLong) { return null; }
        @Override public DeadLetterEvent getById(Long aLong) { return null; }
        @Override public DeadLetterEvent getReferenceById(Long aLong) { return null; }
        @Override public <S extends DeadLetterEvent> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends DeadLetterEvent> List<S> findAll(Example<S> example) { return null; }
        @Override public <S extends DeadLetterEvent> List<S> findAll(Example<S> example, Sort sort) { return null; }
        @Override public <S extends DeadLetterEvent> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends DeadLetterEvent> long count(Example<S> example) { return 0; }
        @Override public <S extends DeadLetterEvent> boolean exists(Example<S> example) { return false; }
        @Override public <S extends DeadLetterEvent, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public Optional<DeadLetterEvent> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public long count() { return events.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(DeadLetterEvent entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends DeadLetterEvent> entities) {}
        @Override public void deleteAll() {}
        @Override public Page<DeadLetterEvent> findAll(Pageable pageable) { return null; }
    }

    private static class StubTriageService implements TriageService {
        boolean selectDepartmentCalled = false;
        boolean throwException = false;

        @Override
        public TriageResponse triage(DialogueCreateRequest request) {
            return new TriageResponse();
        }

        @Override
        public TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName) {
            selectDepartmentCalled = true;
            if (throwException) {
                throw new RuntimeException("Simulated failure");
            }
            return new TriageResponse();
        }
    }
}
