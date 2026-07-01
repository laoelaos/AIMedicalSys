package com.aimedical.modules.commonmodule.store.impl;

import com.aimedical.modules.commonmodule.store.SessionStore;
import com.aimedical.modules.commonmodule.store.SuggestionStore;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@Service
public class ConcurrentHashMapStore implements SuggestionStore {

    private final ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>();

    @Override
    public Object get(String key) { return store.get(key); }

    @Override
    public void put(String key, Object value) { store.put(key, value); }

    @Override
    public Object remove(String key) { return store.remove(key); }

    @Override
    public boolean containsKey(String key) { return store.containsKey(key); }

    @Override
    public Set<String> keySet() { return store.keySet(); }

    @Override
    public Object compute(String key, BiFunction<String, Object, Object> remappingFunction) {
        return store.compute(key, remappingFunction);
    }

    @Override
    public Object createIfNotExists(String key, Object value) {
        return store.putIfAbsent(key, value);
    }
}
