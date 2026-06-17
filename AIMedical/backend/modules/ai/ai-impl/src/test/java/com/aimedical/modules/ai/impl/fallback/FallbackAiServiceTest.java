package com.aimedical.modules.ai.impl.fallback;

import org.junit.jupiter.api.Test;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.ai.api.degradation.DegradationContext;
import com.aimedical.modules.ai.api.degradation.DegradationStrategy;
import com.aimedical.modules.ai.api.dto.triage.TriageRequest;
import com.aimedical.modules.ai.api.dto.triage.TriageResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.LoggerFactory;

class FallbackAiServiceTest {

    @Test
    void shouldDelegateToFirstAvailableService() {
        AiService delegate = mock(AiService.class);
        TriageRequest request = new TriageRequest();
        TriageResponse response = new TriageResponse();
        when(delegate.triage(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<TriageResponse> result = fallback.triage(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void shouldReturnFallbackResultWhenNoDelegateAvailable() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<TriageResponse> result = fallback.triage(new TriageRequest()).join();

        assertFalse(result.isSuccess());
        assertTrue(result.isDegraded());
        assertEquals("No available AiService delegate", result.getFallbackReason());
    }

    @Test
    void shouldDegradeWhenStrategyTriggers() {
        AiService delegate = mock(AiService.class);
        TriageRequest request = new TriageRequest();
        AiResult<TriageResponse> failureResult = AiResult.failure("ERR");
        when(delegate.triage(request)).thenReturn(CompletableFuture.completedFuture(failureResult));

        DegradationStrategy strategy = mock(DegradationStrategy.class);
        when(strategy.shouldDegrade(any())).thenReturn(true);

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of(strategy));
        AiResult<TriageResponse> result = fallback.triage(request).join();

        assertFalse(result.isSuccess());
        assertTrue(result.isDegraded());
        assertEquals("Degraded by strategy", result.getFallbackReason());
    }

    @Test
    void shouldReturnOriginalResultWhenNoStrategyDegrades() {
        AiService delegate = mock(AiService.class);
        TriageRequest request = new TriageRequest();
        AiResult<TriageResponse> failureResult = AiResult.failure("ERR");
        when(delegate.triage(request)).thenReturn(CompletableFuture.completedFuture(failureResult));

        DegradationStrategy strategy = mock(DegradationStrategy.class);
        when(strategy.shouldDegrade(any())).thenReturn(false);

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of(strategy));
        AiResult<TriageResponse> result = fallback.triage(request).join();

        assertFalse(result.isSuccess());
        assertFalse(result.isDegraded());
        assertEquals("ERR", result.getErrorCode());
    }

    @Test
    void shouldExcludeSelfFromDelegates() {
        AiService delegate = mock(AiService.class);
        FallbackAiService outer = new FallbackAiService(List.of(delegate), List.of());
        FallbackAiService inner = new FallbackAiService(List.of(outer, delegate), List.of());

        TriageRequest request = new TriageRequest();
        TriageResponse response = new TriageResponse();
        when(delegate.triage(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        AiResult<TriageResponse> result = inner.triage(request).join();
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldReturnOriginalResultWhenDelegateAlreadyDegraded() {
        AiService delegate = mock(AiService.class);
        TriageRequest request = new TriageRequest();
        AiResult<TriageResponse> degradedResult = AiResult.degraded("already degraded");
        when(delegate.triage(request)).thenReturn(CompletableFuture.completedFuture(degradedResult));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<TriageResponse> result = fallback.triage(request).join();

        assertFalse(result.isSuccess());
        assertTrue(result.isDegraded());
        assertEquals("already degraded", result.getFallbackReason());
    }

    @Test
    void shouldLogErrorOnConstruction() {
        Logger logger = (Logger) LoggerFactory.getLogger(FallbackAiService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            new FallbackAiService(List.of(), List.of());
            assertEquals(1, appender.list.size());
            assertEquals(Level.ERROR, appender.list.get(0).getLevel());
            assertEquals("No available AiService delegate", appender.list.get(0).getFormattedMessage());
        } finally {
            appender.stop();
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldLogWarnOnSubsequentCalls() {
        Logger logger = (Logger) LoggerFactory.getLogger(FallbackAiService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            FallbackAiService service = new FallbackAiService(List.of(), List.of());

            service.triage(new TriageRequest()).join();
            assertEquals(2, appender.list.size());
            assertEquals(Level.WARN, appender.list.get(1).getLevel());
            assertEquals("No available AiService delegate", appender.list.get(1).getFormattedMessage());

            appender.list.clear();

            service.triage(new TriageRequest()).join();
            assertEquals(1, appender.list.size());
            assertEquals(Level.WARN, appender.list.get(0).getLevel());
            assertEquals("No available AiService delegate", appender.list.get(0).getFormattedMessage());
        } finally {
            appender.stop();
            logger.detachAppender(appender);
        }
    }
}
