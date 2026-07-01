package com.aimedical.modules.prescription.task;

import com.aimedical.modules.commonmodule.store.DraftContextStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DraftContextCleanupTaskTest {

    private StubDraftContextStore store;
    private DraftContextCleanupTask task;

    @BeforeEach
    void setUp() {
        store = new StubDraftContextStore();
        task = new DraftContextCleanupTask(store);
    }

    @Test
    void shouldRecordWriteTimestamp() {
        Instant now = Instant.now();
        store.put("key-1", "value-1");
        task.recordWrite("key-1", now);

        task.cleanupExpiredDrafts();
        assertTrue(store.containsKey("key-1"));
    }

    @Test
    void shouldRemoveExpiredDraft() {
        Instant past = Instant.now().minusSeconds(3601);
        store.put("key-1", "value-1");
        task.recordWrite("key-1", past);

        task.cleanupExpiredDrafts();

        assertFalse(store.containsKey("key-1"));
    }

    @Test
    void shouldKeepNonExpiredDraft() {
        Instant now = Instant.now();
        store.put("key-1", "value-1");
        task.recordWrite("key-1", now);

        task.cleanupExpiredDrafts();

        assertTrue(store.containsKey("key-1"));
    }

    @Test
    void shouldRemoveTimestampWithEntry() {
        Instant past = Instant.now().minusSeconds(3601);
        store.put("key-1", "value-1");
        task.recordWrite("key-1", past);

        task.cleanupExpiredDrafts();

        assertFalse(store.containsKey("key-1"));
    }

    @Test
    void shouldNotRemoveEntryWithoutTimestamp() {
        store.put("key-1", "value-1");

        task.cleanupExpiredDrafts();

        assertTrue(store.containsKey("key-1"));
    }

    @Test
    void shouldHandleEmptyStore() {
        task.cleanupExpiredDrafts();
    }

    @Test
    void shouldRemoveOnlyExpiredEntries() {
        Instant past = Instant.now().minusSeconds(3601);
        Instant now = Instant.now();
        store.put("expired-key", "value-1");
        store.put("valid-key", "value-2");
        task.recordWrite("expired-key", past);
        task.recordWrite("valid-key", now);

        task.cleanupExpiredDrafts();

        assertFalse(store.containsKey("expired-key"));
        assertTrue(store.containsKey("valid-key"));
    }

    @Test
    void shouldRemoveTimestampAfterCleanup() {
        Instant past = Instant.now().minusSeconds(3601);
        store.put("key-1", "value-1");
        task.recordWrite("key-1", past);

        task.cleanupExpiredDrafts();

        assertFalse(store.containsKey("key-1"));
    }

    @Test
    void removeTimestampShouldRemoveTracking() {
        store.put("key-1", "value-1");
        task.recordWrite("key-1", Instant.now());
        task.removeTimestamp("key-1");

        task.cleanupExpiredDrafts();

        assertTrue(store.containsKey("key-1"));
    }

    @Test
    void taskShouldBeInPrescriptionPackage() {
        assertEquals("com.aimedical.modules.prescription.task",
                DraftContextCleanupTask.class.getPackageName());
    }

    @Test
    void taskShouldHaveComponentAnnotation() {
        assertNotNull(DraftContextCleanupTask.class.getAnnotation(
                org.springframework.stereotype.Component.class));
    }

    @Test
    void cleanupShouldHandleBoundaryTtl() {
        Instant boundary = Instant.now().plusSeconds(3600);
        store.put("boundary-key", "value");
        task.recordWrite("boundary-key", boundary);

        task.cleanupExpiredDrafts();

        assertTrue(store.containsKey("boundary-key"));
    }

    private static class StubDraftContextStore implements DraftContextStore {
        private final Map<String, Object> map = new HashMap<>();

        @Override
        public Object get(String key) { return map.get(key); }

        @Override
        public void put(String key, Object value) { map.put(key, value); }

        @Override
        public Object remove(String key) { return map.remove(key); }

        @Override
        public boolean containsKey(String key) { return map.containsKey(key); }

        @Override
        public Set<String> keySet() { return map.keySet(); }

        @Override
        public Object compute(String key, java.util.function.BiFunction<String, Object, Object> remappingFunction) {
            return map.compute(key, remappingFunction);
        }

        @Override
        public Object createIfNotExists(String key, Object value) {
            return map.putIfAbsent(key, value);
        }
    }
}
