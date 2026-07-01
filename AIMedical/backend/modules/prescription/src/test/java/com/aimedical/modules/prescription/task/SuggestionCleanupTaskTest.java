package com.aimedical.modules.prescription.task;

import com.aimedical.modules.commonmodule.store.SuggestionStore;
import com.aimedical.modules.commonmodule.store.SuggestionStoreEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class SuggestionCleanupTaskTest {

    private StubSuggestionStore store;
    private SuggestionCleanupTask task;

    @BeforeEach
    void setUp() {
        store = new StubSuggestionStore();
        task = new SuggestionCleanupTask(store);
    }

    @Test
    void shouldRemoveExpiredCompletedAndConsumedEntry() {
        StubEntry entry = new StubEntry("COMPLETED", true, Instant.now().minusSeconds(3601));
        store.put("key-1", entry);

        task.cleanupExpiredSuggestions();

        assertFalse(store.containsKey("key-1"));
    }

    @Test
    void shouldRemoveExpiredFailedEntryEvenIfNotConsumed() {
        StubEntry entry = new StubEntry("FAILED", false, Instant.now().minusSeconds(3601));
        store.put("key-1", entry);

        task.cleanupExpiredSuggestions();

        assertFalse(store.containsKey("key-1"));
    }

    @Test
    void shouldNotRemoveFailedEntryWhenNotExpired() {
        StubEntry entry = new StubEntry("FAILED", false, Instant.now());
        store.put("key-1", entry);

        task.cleanupExpiredSuggestions();

        assertTrue(store.containsKey("key-1"));
    }

    @Test
    void shouldNotRemoveEntryWhenNotExpired() {
        StubEntry entry = new StubEntry("COMPLETED", true, Instant.now());
        store.put("key-1", entry);

        task.cleanupExpiredSuggestions();

        assertTrue(store.containsKey("key-1"));
    }

    @Test
    void shouldNotRemoveEntryWhenNotConsumed() {
        StubEntry entry = new StubEntry("COMPLETED", false, Instant.now().minusSeconds(3601));
        store.put("key-1", entry);

        task.cleanupExpiredSuggestions();

        assertTrue(store.containsKey("key-1"));
    }

    @Test
    void shouldNotRemoveEntryWithStatusPending() {
        StubEntry entry = new StubEntry("PENDING", true, Instant.now().minusSeconds(3601));
        store.put("key-1", entry);

        task.cleanupExpiredSuggestions();

        assertTrue(store.containsKey("key-1"));
    }

    @Test
    void shouldSkipEntryWithWrongType() {
        store.put("key-1", "not-an-entry");

        task.cleanupExpiredSuggestions();

        assertTrue(store.containsKey("key-1"));
    }

    @Test
    void shouldHandleEmptyStore() {
        task.cleanupExpiredSuggestions();
    }

    @Test
    void shouldNotRemoveEntryWhenTimestampIsNull() {
        StubEntry entry = new StubEntry("COMPLETED", true, null);
        store.put("key-1", entry);

        task.cleanupExpiredSuggestions();

        assertTrue(store.containsKey("key-1"));
    }

    @Test
    void shouldRemoveOnlyMatchingEntries() {
        StubEntry expired = new StubEntry("COMPLETED", true, Instant.now().minusSeconds(3601));
        StubEntry valid = new StubEntry("PENDING", false, Instant.now());
        store.put("expired-key", expired);
        store.put("valid-key", valid);

        task.cleanupExpiredSuggestions();

        assertFalse(store.containsKey("expired-key"));
        assertTrue(store.containsKey("valid-key"));
    }

    private static class StubEntry implements SuggestionStoreEntry {
        private final String status;
        private final boolean consumed;
        private final Instant timestamp;

        StubEntry(String status, boolean consumed, Instant timestamp) {
            this.status = status;
            this.consumed = consumed;
            this.timestamp = timestamp;
        }

        @Override
        public String getStatusName() { return status; }

        @Override
        public boolean isConsumed() { return consumed; }

        @Override
        public Instant getTimestamp() { return timestamp; }
    }

    private static class StubSuggestionStore implements SuggestionStore {
        private final ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

        @Override
        public Object compute(String key, BiFunction<String, Object, Object> remappingFunction) {
            return map.compute(key, remappingFunction);
        }

        @Override
        public Object createIfNotExists(String key, Object value) {
            return map.putIfAbsent(key, value);
        }

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
    }
}
