package com.aimedical.modules.consultation;

import com.aimedical.modules.consultation.dto.RecommendedDepartment;
import com.aimedical.modules.consultation.fallback.StaticDepartmentFallbackProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaticDepartmentFallbackProviderTest {

    @Test
    void shouldReturnEmptyListWhenConfigIsBlank() {
        StaticDepartmentFallbackProvider provider = new StaticDepartmentFallbackProvider();
        setField(provider, "fallbackDepartmentsConfig", "");
        List<RecommendedDepartment> result = provider.getFallbackDepartments();
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldParseSingleDepartmentEntry() {
        StaticDepartmentFallbackProvider provider = new StaticDepartmentFallbackProvider();
        setField(provider, "fallbackDepartmentsConfig", "dept-01:内科");
        List<RecommendedDepartment> result = provider.getFallbackDepartments();
        assertEquals(1, result.size());
        assertEquals("dept-01", result.get(0).getDepartmentId());
        assertEquals("内科", result.get(0).getDepartmentName());
    }

    @Test
    void shouldParseMultipleDepartmentEntries() {
        StaticDepartmentFallbackProvider provider = new StaticDepartmentFallbackProvider();
        setField(provider, "fallbackDepartmentsConfig", "dept-01:内科,dept-02:外科");
        List<RecommendedDepartment> result = provider.getFallbackDepartments();
        assertEquals(2, result.size());
        assertEquals("dept-01", result.get(0).getDepartmentId());
        assertEquals("内科", result.get(0).getDepartmentName());
        assertEquals("dept-02", result.get(1).getDepartmentId());
        assertEquals("外科", result.get(1).getDepartmentName());
    }

    @Test
    void shouldSkipMalformedEntries() {
        StaticDepartmentFallbackProvider provider = new StaticDepartmentFallbackProvider();
        setField(provider, "fallbackDepartmentsConfig", "dept-01:内科,invalid-entry,dept-02:外科");
        List<RecommendedDepartment> result = provider.getFallbackDepartments();
        assertEquals(2, result.size());
    }

    @Test
    void shouldDefaultScoreToZero() {
        StaticDepartmentFallbackProvider provider = new StaticDepartmentFallbackProvider();
        setField(provider, "fallbackDepartmentsConfig", "dept-01:内科");
        List<RecommendedDepartment> result = provider.getFallbackDepartments();
        assertEquals(0f, result.get(0).getScore(), 0.001f);
    }

    private void setField(Object target, String fieldName, String value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
