package com.aimedical.modules.commonmodule.store;

import java.util.Set;

public interface SessionStore<K, V> {
    V get(K key);
    void put(K key, V value);
    V remove(K key);
    boolean containsKey(K key);
    Set<K> keySet();
}
