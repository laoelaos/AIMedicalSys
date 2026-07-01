package com.aimedical.modules.commonmodule.store;

public interface DraftContextStore extends SessionStore<String, Object> {
    Object compute(String key, java.util.function.BiFunction<String, Object, Object> remappingFunction);
    Object createIfNotExists(String key, Object value);
}
