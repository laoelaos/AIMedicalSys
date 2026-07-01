package com.aimedical.modules.commonmodule.store.impl;

import com.aimedical.modules.commonmodule.store.DraftContextStore;
import com.aimedical.modules.commonmodule.store.SessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DraftContextStoreImplTest {

    private DraftContextStoreImpl store;

    @BeforeEach
    void setUp() {
        store = new DraftContextStoreImpl();
    }

    @Test
    void shouldImplementDraftContextStore() {
        assertInstanceOf(DraftContextStore.class, store);
    }

    @Test
    void shouldImplementSessionStore() {
        assertInstanceOf(SessionStore.class, store);
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
    void shouldHandleConcurrentPutsAndGets() throws InterruptedException {
        int threadCount = 10;
        int opsPerThread = 100;
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

    @Test
    void shouldNotShareKeyspaceWithConcurrentHashMapStore() {
        ConcurrentHashMapStore otherStore = new ConcurrentHashMapStore();
        store.put("shared-key", "draft-value");
        otherStore.put("shared-key", "suggestion-value");

        assertEquals("draft-value", store.get("shared-key"));
        assertEquals("suggestion-value", otherStore.get("shared-key"));
    }
}
