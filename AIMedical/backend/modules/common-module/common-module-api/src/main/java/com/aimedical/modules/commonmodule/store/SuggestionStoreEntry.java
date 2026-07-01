package com.aimedical.modules.commonmodule.store;

import java.time.Instant;

public interface SuggestionStoreEntry {
    String getStatusName();
    boolean isConsumed();
    Instant getTimestamp();
}
