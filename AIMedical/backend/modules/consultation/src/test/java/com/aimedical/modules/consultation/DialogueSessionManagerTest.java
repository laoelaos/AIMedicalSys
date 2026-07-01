package com.aimedical.modules.consultation;

import com.aimedical.modules.commonmodule.store.SessionStore;
import com.aimedical.modules.consultation.dialogue.DialogueSession;
import com.aimedical.modules.consultation.dialogue.DialogueSessionManager;
import com.aimedical.modules.consultation.entity.TriageRecord;
import com.aimedical.modules.consultation.repository.TriageRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class DialogueSessionManagerTest {

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String NONEXISTENT_UUID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

    private InMemorySessionStore sessionStore;
    private StubTriageRecordRepository recordRepository;
    private DialogueSessionManager manager;

    @BeforeEach
    void setUp() {
        sessionStore = new InMemorySessionStore();
        recordRepository = new StubTriageRecordRepository();
        manager = new DialogueSessionManager(sessionStore, recordRepository);
    }

    @Test
    void shouldCreateNewSession() {
        DialogueSession session = manager.createSession(VALID_UUID);
        assertNotNull(session);
        assertEquals(VALID_UUID, session.getSessionId());
        assertNotNull(session.getCreatedAt());
        assertTrue(sessionStore.containsKey(VALID_UUID));
    }

    @Test
    void shouldReturnExistingSessionWhenCreatingDuplicateSession() {
        DialogueSession first = manager.createSession(VALID_UUID);
        DialogueSession second = manager.createSession(VALID_UUID);
        assertSame(first, second);
    }

    @Test
    void shouldCancelSession() {
        manager.createSession(VALID_UUID);
        manager.cancelSession(VALID_UUID);
        assertFalse(sessionStore.containsKey(VALID_UUID));
    }

    @Test
    void shouldRestoreExistingSession() {
        manager.createSession(VALID_UUID);
        DialogueSession restored = manager.restoreSession(VALID_UUID);
        assertNotNull(restored);
        assertEquals(VALID_UUID, restored.getSessionId());
    }

    @Test
    void shouldReturnNullWhenRestoringNonExistentSession() {
        DialogueSession restored = manager.restoreSession(NONEXISTENT_UUID);
        assertNull(restored);
    }

    @Test
    void shouldRestoreSessionFromTriageRecordWhenNotInStore() {
        TriageRecord record = new TriageRecord();
        record.setSessionId("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");
        record.setChiefComplaint("头痛三天");
        record.setCorrectedChiefComplaint("AI修正：偏头痛");
        record.setRuleVersion("v1.0");
        record.setRuleSetId("RS001");
        recordRepository.record = record;

        DialogueSession restored = manager.restoreSession("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");

        assertNotNull(restored);
        assertEquals("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d", restored.getSessionId());
        assertEquals("AI修正：偏头痛", restored.getCorrectedChiefComplaint());
        assertEquals("头痛三天", restored.getChiefComplaint());
        assertEquals("v1.0", restored.getRuleVersion());
        assertEquals("RS001", restored.getRuleSetId());
    }

    @Test
    void shouldRestoreSessionWithoutCcWhenTriageRecordHasNullCc() {
        TriageRecord record = new TriageRecord();
        record.setSessionId("aabbccdd-eeff-4a0b-8c9d-0a1b2c3d4e5f");
        record.setChiefComplaint("头痛三天");
        record.setCorrectedChiefComplaint(null);
        recordRepository.record = record;

        DialogueSession restored = manager.restoreSession("aabbccdd-eeff-4a0b-8c9d-0a1b2c3d4e5f");

        assertNotNull(restored);
        assertNull(restored.getCorrectedChiefComplaint());
    }

    @Test
    void shouldUpdateLastAccessedAtOnRestore() {
        manager.createSession(VALID_UUID);
        DialogueSession restored = manager.restoreSession(VALID_UUID);
        assertNotNull(restored.getLastAccessedAt());
    }

    @Test
    void shouldProtectRestoreSessionWithSynchronized() throws InterruptedException {
        TriageRecord record = new TriageRecord();
        record.setSessionId("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");
        record.setChiefComplaint("头痛三天");
        record.setCorrectedChiefComplaint("AI修正：偏头痛");
        record.setRuleVersion("v1.0");
        record.setRuleSetId("RS001");
        recordRepository.record = record;

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> error = new AtomicReference<>();
        DialogueSession[] sessions = new DialogueSession[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    sessions[idx] = manager.restoreSession("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        assertNull(error.get(), "Concurrent restoreSession should not throw");
        assertTrue(sessionStore.containsKey("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d"));
        for (DialogueSession s : sessions) {
            assertNotNull(s);
        }
    }

    @Test
    void shouldEvictExpiredSessions() {
        DialogueSession expired = manager.createSession("11111111-1111-4111-8111-111111111111");
        expired.setLastAccessedAt(LocalDateTime.now().minusMinutes(31));

        DialogueSession valid = manager.createSession("22222222-2222-4222-8222-222222222222");
        valid.setLastAccessedAt(LocalDateTime.now());

        manager.evictExpiredSessions();

        assertFalse(sessionStore.containsKey("11111111-1111-4111-8111-111111111111"));
        assertTrue(sessionStore.containsKey("22222222-2222-4222-8222-222222222222"));
    }

    @Test
    void shouldNotEvictNonExpiredSessions() {
        manager.createSession(VALID_UUID);
        manager.evictExpiredSessions();
        assertTrue(sessionStore.containsKey(VALID_UUID));
    }

    @Test
    void shouldHandleConcurrentCreateSessionCalls() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    DialogueSession session = manager.createSession("33333333-3333-4333-8333-333333333333");
                    assertNotNull(session);
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        assertNull(error.get(), "Concurrent createSession should not throw");
        assertTrue(sessionStore.containsKey("33333333-3333-4333-8333-333333333333"));
    }

    @Test
    void shouldReturnSameSessionForConcurrentCreateSessionCalls() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> error = new AtomicReference<>();
        DialogueSession[] sessions = new DialogueSession[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    sessions[idx] = manager.createSession("44444444-4444-4444-8444-444444444444");
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        assertNull(error.get(), "Concurrent createSession should not throw");
        DialogueSession first = sessions[0];
        for (DialogueSession s : sessions) {
            assertSame(first, s);
        }
    }

    @Test
    void shouldThrowWhenCreateSessionWithNullSessionId() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.createSession(null));
    }

    @Test
    void shouldThrowWhenCreateSessionWithInvalidUuid() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.createSession("not-a-uuid"));
    }

    @Test
    void shouldThrowWhenCreateSessionWithNonV4Uuid() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.createSession("550e8400-e29b-11d4-a716-446655440000"));
    }

    @Test
    void shouldThrowWhenRestoreSessionWithNullSessionId() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.restoreSession(null));
    }

    @Test
    void shouldThrowWhenRestoreSessionWithInvalidUuid() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.restoreSession("bad-format"));
    }

    @Test
    void shouldAcceptUppercaseUuidV4() {
        DialogueSession session = manager.createSession("550E8400-E29B-41D4-A716-446655440000");
        assertNotNull(session);
    }

    @Test
    void shouldHandleConcurrentCreateAndRestoreSession() throws InterruptedException {
        String sessionId = "b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e";
        TriageRecord record = new TriageRecord();
        record.setSessionId(sessionId);
        record.setChiefComplaint("头痛三天");
        record.setCorrectedChiefComplaint("AI修正：偏头痛");
        record.setRuleVersion("v1.0");
        record.setRuleSetId("RS001");
        recordRepository.record = record;

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> error = new AtomicReference<>();
        DialogueSession[] sessions = new DialogueSession[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    if (idx % 2 == 0) {
                        sessions[idx] = manager.createSession(sessionId);
                    } else {
                        sessions[idx] = manager.restoreSession(sessionId);
                    }
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        assertNull(error.get(), "Concurrent createSession and restoreSession should not throw");
        assertTrue(sessionStore.containsKey(sessionId));
        for (DialogueSession s : sessions) {
            assertNotNull(s);
        }
    }

    private static class InMemorySessionStore implements SessionStore<String, DialogueSession> {
        private final Map<String, DialogueSession> map = new HashMap<>();

        @Override
        public void put(String key, DialogueSession value) {
            map.put(key, value);
        }

        @Override
        public DialogueSession get(String key) {
            return map.get(key);
        }

        @Override
        public DialogueSession remove(String key) {
            return map.remove(key);
        }

        @Override
        public Set<String> keySet() {
            return map.keySet();
        }

        @Override
        public boolean containsKey(String key) {
            return map.containsKey(key);
        }
    }

    private static class StubTriageRecordRepository implements TriageRecordRepository {
        TriageRecord record;

        @Override
        public Optional<TriageRecord> findTopBySessionIdOrderByTriageTimeDesc(String sessionId) {
            if (record != null && sessionId.equals(record.getSessionId())) {
                return Optional.of(record);
            }
            return Optional.empty();
        }

        @Override
        public Optional<TriageRecord> findBySessionId(String sessionId) { return Optional.empty(); }

        @Override
        public Optional<TriageRecord> findTopByPatientIdOrderByTriageTimeDesc(String patientId) { return Optional.empty(); }

        @Override
        public List<TriageRecord> findBySessionIdIn(List<String> sessionIds) { return Collections.emptyList(); }

        @Override
        public List<TriageRecord> findAll() { return Collections.emptyList(); }

        @Override
        public List<TriageRecord> findAll(Sort sort) { return Collections.emptyList(); }

        @Override
        public List<TriageRecord> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }

        @Override
        public <S extends TriageRecord> List<S> saveAll(Iterable<S> entities) { return null; }

        @Override
        public TriageRecord save(TriageRecord entity) { return entity; }

        @Override
        public void flush() {}

        @Override
        public <S extends TriageRecord> S saveAndFlush(S entity) { return entity; }

        @Override
        public <S extends TriageRecord> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }

        @Override
        public void deleteAllInBatch(Iterable<TriageRecord> entities) {}

        @Override
        public void deleteAllByIdInBatch(Iterable<Long> longs) {}

        @Override
        public void deleteAllInBatch() {}

        @Override
        public TriageRecord getOne(Long aLong) { return null; }

        @Override
        public TriageRecord getById(Long aLong) { return null; }

        @Override
        public TriageRecord getReferenceById(Long aLong) { return null; }

        @Override
        public <S extends TriageRecord> Optional<S> findOne(Example<S> example) { return Optional.empty(); }

        @Override
        public <S extends TriageRecord> List<S> findAll(Example<S> example) { return null; }

        @Override
        public <S extends TriageRecord> List<S> findAll(Example<S> example, Sort sort) { return null; }

        @Override
        public <S extends TriageRecord> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }

        @Override
        public <S extends TriageRecord> long count(Example<S> example) { return 0; }

        @Override
        public <S extends TriageRecord> boolean exists(Example<S> example) { return false; }

        @Override
        public <S extends TriageRecord, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }

        @Override
        public Optional<TriageRecord> findById(Long aLong) { return Optional.empty(); }

        @Override
        public boolean existsById(Long aLong) { return false; }

        @Override
        public long count() { return 0; }

        @Override
        public void deleteById(Long aLong) {}

        @Override
        public void delete(TriageRecord entity) {}

        @Override
        public void deleteAllById(Iterable<? extends Long> longs) {}

        @Override
        public void deleteAll(Iterable<? extends TriageRecord> entities) {}

        @Override
        public void deleteAll() {}

        @Override
        public Page<TriageRecord> findAll(Pageable pageable) { return null; }
    }
}
