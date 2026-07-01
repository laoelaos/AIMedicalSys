package com.aimedical.modules.prescription.service.assist;

import com.aimedical.modules.commonmodule.store.SuggestionStore;
import com.aimedical.modules.prescription.dto.assist.AiSuggestionResult;
import com.aimedical.modules.prescription.dto.assist.AiSuggestionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class DedupTaskSchedulerTest {

    @Mock
    private SuggestionStore suggestionStore;

    private DedupTaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DedupTaskScheduler(suggestionStore);
    }

    @Test
    void shouldCreateNewTaskWhenNoExisting() {
        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(null);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(null);

        String taskId = scheduler.schedule("rx-001");

        assertNotNull(taskId);
        verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class));
        verify(suggestionStore, never()).compute(anyString(), any());
    }

    @Test
    void shouldReusePendingTask() {
        AiSuggestionResult existing = new AiSuggestionResult();
        existing.setTaskId("existing-task");
        existing.setStatus(AiSuggestionStatus.PENDING);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(existing);

        String taskId = scheduler.schedule("rx-001");

        assertEquals("existing-task", taskId);
        verify(suggestionStore, never()).createIfNotExists(anyString(), any());
        verify(suggestionStore, never()).compute(anyString(), any());
        verify(suggestionStore, never()).put(anyString(), any());
    }

    @Test
    void shouldReuseCompletedNotConsumedTask() {
        AiSuggestionResult existing = new AiSuggestionResult();
        existing.setTaskId("existing-task");
        existing.setStatus(AiSuggestionStatus.COMPLETED);
        existing.setConsumed(false);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(existing);

        String taskId = scheduler.schedule("rx-001");

        assertEquals("existing-task", taskId);
        verify(suggestionStore, never()).createIfNotExists(anyString(), any());
        verify(suggestionStore, never()).compute(anyString(), any());
        verify(suggestionStore, never()).put(anyString(), any());
    }

    @Test
    void shouldCreateNewTaskWhenFailed() {
        AiSuggestionResult existing = new AiSuggestionResult();
        existing.setTaskId("failed-task");
        existing.setStatus(AiSuggestionStatus.FAILED);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(existing);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(existing);
        when(suggestionStore.compute(eq("suggestion-dedup:rx-001"), any()))
                .thenAnswer(invocation -> {
                    BiFunction<String, Object, Object> remapping = invocation.getArgument(1);
                    return remapping.apply("suggestion-dedup:rx-001", existing);
                });

        String taskId = scheduler.schedule("rx-001");

        assertNotNull(taskId);
        assertNotEquals("failed-task", taskId);
        verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class));
    }

    @Test
    void shouldCreateNewTaskWhenCompletedAndConsumed() {
        AiSuggestionResult existing = new AiSuggestionResult();
        existing.setTaskId("consumed-task");
        existing.setStatus(AiSuggestionStatus.COMPLETED);
        existing.setConsumed(true);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(existing);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(existing);
        when(suggestionStore.compute(eq("suggestion-dedup:rx-001"), any()))
                .thenAnswer(invocation -> {
                    BiFunction<String, Object, Object> remapping = invocation.getArgument(1);
                    return remapping.apply("suggestion-dedup:rx-001", existing);
                });

        String taskId = scheduler.schedule("rx-001");

        assertNotNull(taskId);
        assertNotEquals("consumed-task", taskId);
        verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class));
    }

    @Test
    void shouldCreateNewTaskWhenNonAiSuggestionResultInStore() {
        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn("not-a-result");
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn("not-a-result");
        when(suggestionStore.compute(eq("suggestion-dedup:rx-001"), any()))
                .thenAnswer(invocation -> {
                    BiFunction<String, Object, Object> remapping = invocation.getArgument(1);
                    return remapping.apply("suggestion-dedup:rx-001", "not-a-result");
                });

        String taskId = scheduler.schedule("rx-001");

        assertNotNull(taskId);
        verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class));
    }

    @Test
    void shouldReuseExistingTaskWhenComputeFindsReusableValue() {
        AiSuggestionResult failedExisting = new AiSuggestionResult();
        failedExisting.setTaskId("failed-task");
        failedExisting.setStatus(AiSuggestionStatus.FAILED);

        AiSuggestionResult winnerResult = new AiSuggestionResult();
        winnerResult.setTaskId("winner-task");
        winnerResult.setStatus(AiSuggestionStatus.PENDING);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(failedExisting);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(failedExisting);
        when(suggestionStore.compute(eq("suggestion-dedup:rx-001"), any()))
                .thenReturn(winnerResult);

        String taskId = scheduler.schedule("rx-001");

        assertEquals("winner-task", taskId);
        verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenComputeReturnsNonAiSuggestionResult() {
        AiSuggestionResult existing = new AiSuggestionResult();
        existing.setTaskId("failed-task");
        existing.setStatus(AiSuggestionStatus.FAILED);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(existing);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(existing);
        when(suggestionStore.compute(eq("suggestion-dedup:rx-001"), any()))
                .thenReturn("unexpected-type");

        assertThrows(IllegalStateException.class, () -> scheduler.schedule("rx-001"));
    }

    @Test
    void shouldReuseTaskWhenFastPathReturnsCompletedUnconsumed() {
        AiSuggestionResult existing = new AiSuggestionResult();
        existing.setTaskId("reusable-task");
        existing.setStatus(AiSuggestionStatus.COMPLETED);
        existing.setConsumed(false);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(existing);

        String taskId = scheduler.schedule("rx-001");

        assertEquals("reusable-task", taskId);
        verify(suggestionStore, never()).createIfNotExists(anyString(), any());
        verify(suggestionStore, never()).compute(anyString(), any());
    }

    @Test
    void shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsPending() {
        AiSuggestionResult pendingExisting = new AiSuggestionResult();
        pendingExisting.setTaskId("pending-task");
        pendingExisting.setStatus(AiSuggestionStatus.PENDING);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(null);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(pendingExisting);

        String taskId = scheduler.schedule("rx-001");

        assertEquals("pending-task", taskId);
        verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class));
        verify(suggestionStore, never()).compute(anyString(), any());
    }

    @Test
    void shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsCompletedUnconsumed() {
        AiSuggestionResult completedExisting = new AiSuggestionResult();
        completedExisting.setTaskId("completed-task");
        completedExisting.setStatus(AiSuggestionStatus.COMPLETED);
        completedExisting.setConsumed(false);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(null);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(completedExisting);

        String taskId = scheduler.schedule("rx-001");

        assertEquals("completed-task", taskId);
        verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class));
        verify(suggestionStore, never()).compute(anyString(), any());
    }

    @Test
    void shouldReuseProcessingTask() {
        AiSuggestionResult existing = new AiSuggestionResult();
        existing.setTaskId("processing-task");
        existing.setStatus(AiSuggestionStatus.PROCESSING);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(existing);

        String taskId = scheduler.schedule("rx-001");

        assertEquals("processing-task", taskId);
        verify(suggestionStore, never()).createIfNotExists(anyString(), any());
        verify(suggestionStore, never()).compute(anyString(), any());
        verify(suggestionStore, never()).put(anyString(), any());
    }

    @Test
    void shouldCreateNewTaskWhenTimeout() {
        AiSuggestionResult existing = new AiSuggestionResult();
        existing.setTaskId("timeout-task");
        existing.setStatus(AiSuggestionStatus.TIMEOUT);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(existing);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(existing);
        when(suggestionStore.compute(eq("suggestion-dedup:rx-001"), any()))
                .thenAnswer(invocation -> {
                    BiFunction<String, Object, Object> remapping = invocation.getArgument(1);
                    return remapping.apply("suggestion-dedup:rx-001", existing);
                });

        String taskId = scheduler.schedule("rx-001");

        assertNotNull(taskId);
        assertNotEquals("timeout-task", taskId);
        verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class));
    }

    @Test
    void shouldReuseProcessingTaskViaCreateIfNotExists() {
        AiSuggestionResult processingExisting = new AiSuggestionResult();
        processingExisting.setTaskId("processing-task");
        processingExisting.setStatus(AiSuggestionStatus.PROCESSING);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(null);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(processingExisting);

        String taskId = scheduler.schedule("rx-001");

        assertEquals("processing-task", taskId);
        verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class));
        verify(suggestionStore, never()).compute(anyString(), any());
    }

    @Test
    void shouldReuseProcessingTaskViaCompute() {
        AiSuggestionResult failedExisting = new AiSuggestionResult();
        failedExisting.setTaskId("failed-task");
        failedExisting.setStatus(AiSuggestionStatus.FAILED);

        AiSuggestionResult processingResult = new AiSuggestionResult();
        processingResult.setTaskId("processing-task");
        processingResult.setStatus(AiSuggestionStatus.PROCESSING);

        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(failedExisting);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(failedExisting);
        when(suggestionStore.compute(eq("suggestion-dedup:rx-001"), any()))
                .thenReturn(processingResult);

        String taskId = scheduler.schedule("rx-001");

        assertEquals("processing-task", taskId);
        verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class));
    }

    @Test
    void shouldPutCandidateTaskIdKeyWhenCreateNewTask() {
        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(null);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(null);

        String taskId = scheduler.schedule("rx-001");

        assertNotNull(taskId);
        verify(suggestionStore).put(eq(taskId), any(AiSuggestionResult.class));
    }

    @Test
    void shouldGuaranteeCandidateTaskIdVisibilityAfterCreateIfNotExists() {
        when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(null);
        when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
                .thenReturn(null);

        InOrder inOrder = inOrder(suggestionStore);
        scheduler.schedule("rx-001");
        inOrder.verify(suggestionStore).put(anyString(), any(AiSuggestionResult.class));
        inOrder.verify(suggestionStore).createIfNotExists(anyString(), any(AiSuggestionResult.class));
    }
}
