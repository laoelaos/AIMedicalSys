package com.aimedical.modules.consultation;

import com.aimedical.modules.commonmodule.event.RegistrationEvent;
import com.aimedical.modules.consultation.dto.DialogueCreateRequest;
import com.aimedical.modules.consultation.dto.TriageResponse;
import com.aimedical.modules.consultation.entity.DeadLetterEvent;
import com.aimedical.modules.consultation.entity.TriageRecord;
import com.aimedical.modules.consultation.event.RegistrationEventListener;
import com.aimedical.modules.consultation.repository.DeadLetterEventRepository;
import com.aimedical.modules.consultation.repository.TriageRecordRepository;
import com.aimedical.modules.consultation.service.TriageService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationEventListenerTest {

    private StubTriageRecordRepository triageRecordRepository;
    private StubDeadLetterEventRepository deadLetterEventRepository;
    private StubTriageService triageService;
    private ObjectMapper objectMapper;
    private RegistrationEventListener listener;

    @BeforeEach
    void setUp() {
        triageRecordRepository = new StubTriageRecordRepository();
        deadLetterEventRepository = new StubDeadLetterEventRepository();
        triageService = new StubTriageService();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        listener = new RegistrationEventListener(triageRecordRepository, deadLetterEventRepository, objectMapper, triageService);
    }

    @Test
    void shouldDelegateToTriageServiceWhenRecordExistsAndFinalIsNull() {
        TriageRecord record = new TriageRecord();
        record.setSessionId("session-001");
        record.setFinalDepartmentId(null);
        triageRecordRepository.record = record;

        RegistrationEvent event = new RegistrationEvent(1L, "P001", "session-001",
                "dept-01", "神经内科", 100L, LocalDateTime.now());

        listener.handleRegistrationEvent(event);

        assertTrue(triageService.selectDepartmentCalled);
        assertEquals("session-001", triageService.capturedSessionId);
        assertEquals("dept-01", triageService.capturedDepartmentId);
        assertEquals("神经内科", triageService.capturedDepartmentName);
    }

    @Test
    void shouldNotCallTriageServiceWhenFinalDepartmentAlreadySet() {
        TriageRecord record = new TriageRecord();
        record.setSessionId("session-001");
        record.setFinalDepartmentId("existing-dept");
        triageRecordRepository.record = record;

        RegistrationEvent event = new RegistrationEvent(1L, "P001", "session-001",
                "dept-01", "神经内科", 100L, LocalDateTime.now());

        listener.handleRegistrationEvent(event);

        assertFalse(triageService.selectDepartmentCalled);
    }

    @Test
    void shouldDoNothingWhenRecordNotFound() {
        RegistrationEvent event = new RegistrationEvent(1L, "P001", "non-existent",
                "dept-01", "神经内科", 100L, LocalDateTime.now());

        listener.handleRegistrationEvent(event);

        assertFalse(triageService.selectDepartmentCalled);
    }

    @Test
    void shouldSkipWhenSessionIdIsNull() {
        Logger listenerLogger = (Logger) org.slf4j.LoggerFactory.getLogger(RegistrationEventListener.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        listenerLogger.addAppender(appender);

        RegistrationEvent event = new RegistrationEvent(1L, "P001", null,
                "dept-01", "神经内科", 100L, LocalDateTime.now());

        listener.handleRegistrationEvent(event);

        assertFalse(triageService.selectDepartmentCalled);
        assertEquals(1, appender.list.size());
        ILoggingEvent logEvent = appender.list.get(0);
        assertEquals(Level.WARN, logEvent.getLevel());
        assertTrue(logEvent.getFormattedMessage().contains("null sessionId"));

        listenerLogger.detachAppender(appender);
    }

    @Test
    void shouldWriteDeadLetterEventOnRecover() {
        RegistrationEvent event = new RegistrationEvent(1L, "P001", "session-001",
                "dept-01", "神经内科", 100L, LocalDateTime.now());

        listener.recover(new RuntimeException("测试异常"), event);

        assertNotNull(deadLetterEventRepository.savedEvent);
        assertEquals("FAILED", deadLetterEventRepository.savedEvent.getState());
        assertNotNull(deadLetterEventRepository.savedEvent.getFailReason());
        assertNotNull(deadLetterEventRepository.savedEvent.getEventPayload());
    }

    @Test
    void shouldContainAllSevenFieldsInEventPayloadOnRecover() throws Exception {
        LocalDateTime eventTime = LocalDateTime.of(2026, 6, 30, 10, 0);
        RegistrationEvent event = new RegistrationEvent(1L, "P001", "session-001",
                "dept-01", "神经内科", 100L, eventTime);

        listener.recover(new RuntimeException("测试异常"), event);

        String payload = deadLetterEventRepository.savedEvent.getEventPayload();
        Map<String, Object> parsed = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        assertEquals(1, ((Number) parsed.get("registrationId")).longValue());
        assertEquals("P001", parsed.get("patientId"));
        assertEquals("session-001", parsed.get("sessionId"));
        assertEquals("dept-01", parsed.get("departmentId"));
        assertEquals("神经内科", parsed.get("departmentName"));
        assertEquals(100L, ((Number) parsed.get("doctorId")).longValue());
        assertNotNull(parsed.get("eventTime"));
    }

    @Test
    void shouldUseFallbackPayloadWhenSerializationFails() throws Exception {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonParseException(null, "Simulated failure");
            }
        };
        RegistrationEventListener listenerWithFailingMapper = new RegistrationEventListener(
                triageRecordRepository, deadLetterEventRepository, failingMapper, triageService);

        RegistrationEvent event = new RegistrationEvent(1L, "P001", "session-001",
                "dept-01", "神经内科", 100L, LocalDateTime.now());

        listenerWithFailingMapper.recover(new RuntimeException("测试异常"), event);

        assertNotNull(deadLetterEventRepository.savedEvent);
        String payload = deadLetterEventRepository.savedEvent.getEventPayload();
        assertTrue(payload.contains("\"sessionId\":\"session-001\""));
    }

    @Test
    void shouldUseUnknownFailureReasonWhenExceptionMessageIsNull() {
        RegistrationEvent event = new RegistrationEvent(1L, "P001", "session-001",
                "dept-01", "神经内科", 100L, LocalDateTime.now());

        listener.recover(new RuntimeException(), event);

        assertNotNull(deadLetterEventRepository.savedEvent);
        assertEquals("Unknown failure reason", deadLetterEventRepository.savedEvent.getFailReason());
    }

    @Test
    void shouldUseUnknownSessionIdInFallbackPayloadWhenSessionIdIsNull() throws Exception {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonParseException(null, "Simulated failure");
            }
        };
        RegistrationEventListener listenerWithFailingMapper = new RegistrationEventListener(
                triageRecordRepository, deadLetterEventRepository, failingMapper, triageService);

        RegistrationEvent event = new RegistrationEvent(1L, "P001", null,
                "dept-01", "神经内科", 100L, LocalDateTime.now());

        listenerWithFailingMapper.recover(new RuntimeException("测试异常"), event);

        assertNotNull(deadLetterEventRepository.savedEvent);
        String payload = deadLetterEventRepository.savedEvent.getEventPayload();
        assertTrue(payload.contains("\"sessionId\":\"unknown\""));
    }

    @Test
    void shouldSetFailReasonInDeadLetterEvent() {
        RegistrationEvent event = new RegistrationEvent(1L, "P001", "session-001",
                "dept-01", "神经内科", 100L, LocalDateTime.now());

        listener.recover(new RuntimeException("连接超时"), event);

        assertTrue(deadLetterEventRepository.savedEvent.getFailReason().contains("连接超时"));
    }

    private static class StubTriageRecordRepository implements TriageRecordRepository {
        TriageRecord record;

        @Override
        public Optional<TriageRecord> findBySessionId(String sessionId) {
            if (record != null && sessionId.equals(record.getSessionId())) {
                return Optional.of(record);
            }
            return Optional.empty();
        }

        @Override
        public TriageRecord save(TriageRecord entity) {
            this.record = entity;
            return entity;
        }

        @Override
        public Optional<TriageRecord> findTopBySessionIdOrderByTriageTimeDesc(String sessionId) {
            if (record != null && sessionId.equals(record.getSessionId())) {
                return Optional.of(record);
            }
            return Optional.empty();
        }

        @Override
        public Optional<TriageRecord> findTopByPatientIdOrderByTriageTimeDesc(String patientId) {
            return Optional.empty();
        }

        @Override
        public List<TriageRecord> findBySessionIdIn(List<String> sessionIds) {
            return Collections.emptyList();
        }

        @Override public List<TriageRecord> findAll() { return Collections.emptyList(); }
        @Override public List<TriageRecord> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public List<TriageRecord> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public <S extends TriageRecord> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public void flush() {}
        @Override public <S extends TriageRecord> S saveAndFlush(S entity) { return entity; }
        @Override public <S extends TriageRecord> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<TriageRecord> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public TriageRecord getOne(Long aLong) { return null; }
        @Override public TriageRecord getById(Long aLong) { return null; }
        @Override public TriageRecord getReferenceById(Long aLong) { return null; }
        @Override public <S extends TriageRecord> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends TriageRecord> List<S> findAll(Example<S> example) { return null; }
        @Override public <S extends TriageRecord> List<S> findAll(Example<S> example, Sort sort) { return null; }
        @Override public <S extends TriageRecord> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends TriageRecord> long count(Example<S> example) { return 0; }
        @Override public <S extends TriageRecord> boolean exists(Example<S> example) { return false; }
        @Override public <S extends TriageRecord, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public Optional<TriageRecord> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(TriageRecord entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends TriageRecord> entities) {}
        @Override public void deleteAll() {}
        @Override public Page<TriageRecord> findAll(Pageable pageable) { return null; }
    }

    private static class StubDeadLetterEventRepository implements DeadLetterEventRepository {
        DeadLetterEvent savedEvent;

        @Override
        public DeadLetterEvent save(DeadLetterEvent entity) {
            this.savedEvent = entity;
            return entity;
        }

        @Override
        public List<DeadLetterEvent> findByCompensableEvents(String state) {
            return Collections.emptyList();
        }

        @Override public List<DeadLetterEvent> findAll() { return Collections.emptyList(); }
        @Override public List<DeadLetterEvent> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public List<DeadLetterEvent> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
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
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(DeadLetterEvent entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends DeadLetterEvent> entities) {}
        @Override public void deleteAll() {}
        @Override public Page<DeadLetterEvent> findAll(Pageable pageable) { return null; }
    }

    private static class StubTriageService implements TriageService {
        boolean selectDepartmentCalled = false;
        String capturedSessionId;
        String capturedDepartmentId;
        String capturedDepartmentName;

        @Override
        public TriageResponse triage(DialogueCreateRequest request) {
            return new TriageResponse();
        }

        @Override
        public TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName) {
            selectDepartmentCalled = true;
            capturedSessionId = sessionId;
            capturedDepartmentId = departmentId;
            capturedDepartmentName = departmentName;
            return new TriageResponse();
        }
    }
}
