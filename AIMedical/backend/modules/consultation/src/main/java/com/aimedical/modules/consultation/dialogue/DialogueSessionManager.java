package com.aimedical.modules.consultation.dialogue;

import com.aimedical.modules.commonmodule.store.SessionStore;
import com.aimedical.modules.consultation.entity.TriageRecord;
import com.aimedical.modules.consultation.repository.TriageRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class DialogueSessionManager {

    private static final Logger log = LoggerFactory.getLogger(DialogueSessionManager.class);
    private static final long SESSION_TTL_MINUTES = 30;
    private static final Pattern UUID_V4_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);

    private final SessionStore<String, DialogueSession> sessionStore;
    private final TriageRecordRepository triageRecordRepository;

    public DialogueSessionManager(SessionStore<String, DialogueSession> sessionStore,
                                   TriageRecordRepository triageRecordRepository) {
        this.sessionStore = sessionStore;
        this.triageRecordRepository = triageRecordRepository;
    }

    public synchronized DialogueSession createSession(String sessionId) {
        if (sessionId == null || !UUID_V4_PATTERN.matcher(sessionId).matches()) {
            throw new IllegalArgumentException("Invalid UUID v4 format for sessionId: " + sessionId);
        }
        if (sessionStore.containsKey(sessionId)) {
            log.warn("Session already exists for sessionId: {}, returning existing session", sessionId);
            return sessionStore.get(sessionId);
        }
        DialogueSession session = new DialogueSession(sessionId);
        sessionStore.put(sessionId, session);
        return session;
    }

    public void cancelSession(String sessionId) {
        sessionStore.remove(sessionId);
    }

    public synchronized DialogueSession restoreSession(String sessionId) {
        if (sessionId == null || !UUID_V4_PATTERN.matcher(sessionId).matches()) {
            throw new IllegalArgumentException("Invalid UUID v4 format for sessionId: " + sessionId);
        }
        DialogueSession session = sessionStore.get(sessionId);
        if (session != null) {
            session.setLastAccessedAt(LocalDateTime.now());
            return session;
        }
        Optional<TriageRecord> latestRecord = triageRecordRepository
                .findTopBySessionIdOrderByTriageTimeDesc(sessionId);
        if (latestRecord.isPresent()) {
            TriageRecord record = latestRecord.get();
            session = new DialogueSession(sessionId);
            session.setCorrectedChiefComplaint(record.getCorrectedChiefComplaint());
            session.setChiefComplaint(record.getChiefComplaint());
            session.setRuleVersion(record.getRuleVersion());
            session.setRuleSetId(record.getRuleSetId());
            sessionStore.put(sessionId, session);
        }
        return session;
    }

    @Scheduled(fixedRate = 300000)
    public void evictExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        for (String key : new ArrayList<>(sessionStore.keySet())) {
            DialogueSession session = sessionStore.get(key);
            if (session != null && session.getLastAccessedAt() != null
                    && session.getLastAccessedAt().plusMinutes(SESSION_TTL_MINUTES).isBefore(now)) {
                sessionStore.remove(key);
            }
        }
    }
}
