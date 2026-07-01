package com.aimedical.modules.consultation;

import com.aimedical.modules.consultation.dialogue.DialogueSession;
import com.aimedical.modules.consultation.dto.AdditionalResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DialogueSessionTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DialogueSession session = new DialogueSession();
        assertNull(session.getSessionId());
        assertNull(session.getChiefComplaint());
        assertNull(session.getCreatedAt());
        assertNull(session.getLastAccessedAt());
        assertEquals(0, session.getAiFailCount());
        assertEquals(0, session.getRoundCount());
    }

    @Test
    void shouldCreateWithSessionIdConstructor() {
        DialogueSession session = new DialogueSession("session-001");
        assertEquals("session-001", session.getSessionId());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getLastAccessedAt());
    }

    @Test
    void shouldSetAndGetAllFields() {
        DialogueSession session = new DialogueSession();
        session.setSessionId("session-001");
        session.setChiefComplaint("头痛三天");
        session.setCorrectedChiefComplaint("头痛三天，伴有恶心");
        session.setAiFailCount(2);
        session.setRoundCount(3);
        session.setRuleVersion("v1.0");
        session.setRuleSetId("RS001");
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setLastAccessedAt(now);

        assertEquals("session-001", session.getSessionId());
        assertEquals("头痛三天", session.getChiefComplaint());
        assertEquals("头痛三天，伴有恶心", session.getCorrectedChiefComplaint());
        assertEquals(2, session.getAiFailCount());
        assertEquals(3, session.getRoundCount());
        assertEquals("v1.0", session.getRuleVersion());
        assertEquals("RS001", session.getRuleSetId());
        assertEquals(now, session.getCreatedAt());
        assertEquals(now, session.getLastAccessedAt());
    }

    @Test
    void shouldSetAndGetAdditionalResponses() {
        DialogueSession session = new DialogueSession();
        List<AdditionalResponse> responses = new ArrayList<>();
        responses.add(new AdditionalResponse("q1", "a1", null));
        session.setAdditionalResponses(responses);
        assertEquals(1, session.getAdditionalResponses().size());
        assertEquals("q1", session.getAdditionalResponses().get(0).getQuestion());
    }

    @Test
    void shouldDefaultAdditionalResponsesToEmptyList() {
        DialogueSession session = new DialogueSession();
        assertNotNull(session.getAdditionalResponses());
        assertTrue(session.getAdditionalResponses().isEmpty());
    }

    @Test
    void shouldHandleConcurrentReadsAndWrites() throws InterruptedException {
        DialogueSession session = new DialogueSession("session-001");
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    session.setChiefComplaint("complaint-" + idx);
                    session.getChiefComplaint();
                    session.setCorrectedChiefComplaint("corrected-" + idx);
                    session.getCorrectedChiefComplaint();
                    session.setAiFailCount(idx);
                    session.getAiFailCount();
                    session.setRoundCount(idx);
                    session.getRoundCount();
                    session.setRuleVersion("v" + idx);
                    session.getRuleVersion();
                    session.setRuleSetId("RS" + idx);
                    session.getRuleSetId();
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        assertNull(error.get(), "Concurrent access should not throw");
    }

    @Test
    void shouldHandleConcurrentAdditionalResponsesModification() throws InterruptedException {
        DialogueSession session = new DialogueSession("session-001");
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    List<AdditionalResponse> list = session.getAdditionalResponses();
                    list.add(new AdditionalResponse("q" + idx, "a" + idx, null));
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        assertNull(error.get(), "Concurrent add to CopyOnWriteArrayList should not throw");
        assertEquals(threadCount, session.getAdditionalResponses().size());
    }

    @Test
    void shouldSupportAtomicIntegerStateTransitions() {
        DialogueSession session = new DialogueSession();
        assertEquals(0, session.getAiFailCount());
        assertEquals(0, session.getRoundCount());

        session.setAiFailCount(5);
        assertEquals(5, session.getAiFailCount());

        session.setRoundCount(3);
        assertEquals(3, session.getRoundCount());
    }
}
