package com.aimedical.modules.commonmodule.store;

import java.util.Set;
import java.util.function.BiFunction;

public interface SuggestionStore extends SessionStore<String, Object> {
    Object compute(String key, BiFunction<String, Object, Object> remappingFunction);
    Object createIfNotExists(String key, Object value);
}
