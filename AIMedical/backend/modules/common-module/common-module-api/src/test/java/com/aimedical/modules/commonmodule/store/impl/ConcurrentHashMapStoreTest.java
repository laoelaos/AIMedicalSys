package com.aimedical.modules.commonmodule.store.impl;

import com.aimedical.modules.commonmodule.store.DraftContextStore;
import com.aimedical.modules.commonmodule.store.SessionStore;
import com.aimedical.modules.commonmodule.store.SuggestionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentHashMapStoreTest {

    @Test
    void shouldBeAnnotatedWithService() {
        assertNotNull(ConcurrentHashMapStore.class.getAnnotation(org.springframework.stereotype.Service.class));
    }

    private ConcurrentHashMapStore store;

    @BeforeEach
    void setUp() {
        store = new ConcurrentHashMapStore();
    }

    @Test
    void shouldReturnNullWhenKeyNotFound() {
        assertNull(store.get("nonexistent"));
    }

    @Test
    void shouldReturnValueAfterPut() {
        store.put("key1", "value1");
        assertEquals("value1", store.get("key1"));
    }

    @Test
    void shouldOverwriteExistingValueOnPut() {
        store.put("key1", "value1");
        store.put("key1", "value2");
        assertEquals("value2", store.get("key1"));
    }

    @Test
    void shouldReturnRemovedValue() {
        store.put("key1", "value1");
        assertEquals("value1", store.remove("key1"));
    }

    @Test
    void shouldReturnNullWhenRemovingNonExistentKey() {
        assertNull(store.remove("nonexistent"));
    }

    @Test
    void shouldRemoveEntry() {
        store.put("key1", "value1");
        store.remove("key1");
        assertNull(store.get("key1"));
    }

    @Test
    void shouldContainKeyAfterPut() {
        store.put("key1", "value1");
        assertTrue(store.containsKey("key1"));
    }

    @Test
    void shouldNotContainKeyAfterRemove() {
        store.put("key1", "value1");
        store.remove("key1");
        assertFalse(store.containsKey("key1"));
    }

    @Test
    void shouldReturnEmptyKeySetInitially() {
        assertTrue(store.keySet().isEmpty());
    }

    @Test
    void shouldReturnAllKeysInKeySet() {
        store.put("key1", "v1");
        store.put("key2", "v2");
        assertEquals(Set.of("key1", "key2"), store.keySet());
    }

    @Test
    void shouldReflectRemoveInKeySet() {
        store.put("key1", "v1");
        store.put("key2", "v2");
        store.remove("key1");
        assertEquals(Set.of("key2"), store.keySet());
    }

    @Test
    void shouldComputeNewValue() {
        store.put("k", "old");
        assertEquals("new_old", store.compute("k", (k, v) -> "new_" + v));
        assertEquals("new_old", store.get("k"));
    }

    @Test
    void shouldComputeCreatesEntryWhenKeyAbsent() {
        assertEquals("created", store.compute("k", (k, v) -> "created"));
        assertEquals("created", store.get("k"));
    }

    @Test
    void shouldDeleteEntryWhenComputeReturnsNull() {
        store.put("k", "v");
        assertNull(store.compute("k", (k, v) -> null));
        assertFalse(store.containsKey("k"));
    }

    @Test
    void shouldRemainAbsentWhenComputeReturnsNullOnMissingKey() {
        assertNull(store.compute("k", (k, v) -> null));
        assertFalse(store.containsKey("k"));
    }

    @Test
    void shouldPassCorrectArgumentsToRemappingFunction() {
        store.put("k", "v");
        store.compute("k", (k, v) -> {
            assertEquals("k", k);
            assertEquals("v", v);
            return "updated";
        });
    }

    @Test
    void shouldImplementSessionStore() {
        assertInstanceOf(SessionStore.class, store);
    }

    @Test
    void shouldImplementSuggestionStore() {
        assertInstanceOf(SuggestionStore.class, store);
    }

    @Test
    void shouldCreateIfNotExistsWhenKeyAbsent() {
        assertNull(store.createIfNotExists("key1", "value1"));
        assertEquals("value1", store.get("key1"));
    }

    @Test
    void shouldReturnOldValueWhenCreateIfNotExistsOnExistingKey() {
        store.put("key1", "old");
        assertEquals("old", store.createIfNotExists("key1", "new"));
        assertEquals("old", store.get("key1"));
    }

    @Test
    void shouldThrowNpeWhenGetWithNullKey() {
        assertThrows(NullPointerException.class, () -> store.get(null));
    }

    @Test
    void shouldThrowNpeWhenPutWithNullKey() {
        assertThrows(NullPointerException.class, () -> store.put(null, "v"));
    }

    @Test
    void shouldThrowNpeWhenPutWithNullValue() {
        assertThrows(NullPointerException.class, () -> store.put("k", null));
    }

    @Test
    void shouldThrowNpeWhenRemoveWithNullKey() {
        assertThrows(NullPointerException.class, () -> store.remove(null));
    }

    @Test
    void shouldThrowNpeWhenContainsKeyWithNullKey() {
        assertThrows(NullPointerException.class, () -> store.containsKey(null));
    }

    @Test
    void shouldThrowNpeWhenComputeWithNullKey() {
        assertThrows(NullPointerException.class, () -> store.compute(null, (k, v) -> v));
    }

    @Test
    void shouldThrowNpeWhenComputeWithNullFunction() {
        assertThrows(NullPointerException.class, () -> store.compute("k", null));
    }

    @Test
    void shouldNotImplementDraftContextStore() {
        assertFalse(store instanceof DraftContextStore);
    }

    @Test
    void shouldThrowNpeWhenCreateIfNotExistsWithNullKey() {
        assertThrows(NullPointerException.class, () -> store.createIfNotExists(null, "value"));
    }

    @Test
    void shouldThrowNpeWhenCreateIfNotExistsWithNullValue() {
        assertThrows(NullPointerException.class, () -> store.createIfNotExists("key", null));
    }

    @Test
    void shouldHandleConcurrentCreateIfNotExists() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    return;
                }
                Object old = store.createIfNotExists("race-key", "value-" + threadId);
                if (old == null) {
                    successCount.incrementAndGet();
                }
                doneLatch.countDown();
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();
        assertEquals(1, successCount.get());
        assertNotNull(store.get("race-key"));
    }

    @Test
    void shouldHandleConcurrentPutsAndGets() throws InterruptedException {
        int threadCount = 10;
        int opsPerThread = 100;
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int threadId = i;
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    String key = "key-" + threadId + "-" + j;
                    store.put(key, "val-" + threadId + "-" + j);
                    assertNotNull(store.get(key));
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(threadCount * opsPerThread, store.keySet().size());
    }
}
