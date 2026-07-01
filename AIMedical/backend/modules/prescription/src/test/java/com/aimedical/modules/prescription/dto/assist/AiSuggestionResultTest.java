package com.aimedical.modules.prescription.dto.assist;

import com.aimedical.modules.commonmodule.store.SuggestionStoreEntry;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class AiSuggestionResultTest {

    @Test
    void shouldSetAndGetFields() {
        LocalDateTime now = LocalDateTime.now();
        AiSuggestionResult result = new AiSuggestionResult();
        result.setTaskId("task-001");
        result.setSuggestion("{\"drugs\":[]}");
        result.setStatus(AiSuggestionStatus.COMPLETED);
        result.setCreateTime(now);
        result.setFailReason("超时");
        result.setConsumed(true);
        result.setPartialData("部分结果");

        assertEquals("task-001", result.getTaskId());
        assertEquals("{\"drugs\":[]}", result.getSuggestion());
        assertEquals(AiSuggestionStatus.COMPLETED, result.getStatus());
        assertEquals(now, result.getCreateTime());
        assertEquals("超时", result.getFailReason());
        assertTrue(result.isConsumed());
        assertEquals("部分结果", result.getPartialData());
    }

    @Test
    void shouldImplementSuggestionStoreEntry() {
        AiSuggestionResult result = new AiSuggestionResult();
        assertInstanceOf(SuggestionStoreEntry.class, result);
    }

    @Test
    void shouldReturnStatusNameFromStatusEnum() {
        AiSuggestionResult result = new AiSuggestionResult();
        result.setStatus(AiSuggestionStatus.COMPLETED);
        assertEquals("COMPLETED", result.getStatusName());

        result.setStatus(AiSuggestionStatus.PENDING);
        assertEquals("PENDING", result.getStatusName());

        result.setStatus(AiSuggestionStatus.FAILED);
        assertEquals("FAILED", result.getStatusName());
    }

    @Test
    void shouldReturnNullStatusNameWhenStatusIsNull() {
        AiSuggestionResult result = new AiSuggestionResult();
        assertNull(result.getStatusName());
    }

    @Test
    void shouldReturnConsumedFromField() {
        AiSuggestionResult result = new AiSuggestionResult();
        result.setConsumed(true);
        assertTrue(result.isConsumed());

        result.setConsumed(false);
        assertFalse(result.isConsumed());
    }

    @Test
    void shouldReturnTimestampFromCreateTime() {
        LocalDateTime createTime = LocalDateTime.of(2026, 6, 30, 12, 0, 0);
        AiSuggestionResult result = new AiSuggestionResult();
        result.setCreateTime(createTime);

        Instant expected = createTime.toInstant(ZoneOffset.UTC);
        assertEquals(expected, result.getTimestamp());
    }

    @Test
    void shouldReturnNullTimestampWhenCreateTimeIsNull() {
        AiSuggestionResult result = new AiSuggestionResult();
        assertNull(result.getTimestamp());
    }
}
