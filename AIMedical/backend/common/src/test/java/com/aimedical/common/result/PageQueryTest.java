package com.aimedical.common.result;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PageQueryTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static <T> Set<String> violationsAsMessages(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void shouldUseDefaultValues() {
        PageQuery query = new PageQuery();
        assertEquals(0, query.getPage());
        assertEquals(20, query.getSize());
        assertNull(query.getSort());
    }

    @Test
    void shouldSetAndGetPage() {
        PageQuery query = new PageQuery();
        query.setPage(2);
        assertEquals(2, query.getPage());
    }

    @Test
    void shouldSetAndGetSize() {
        PageQuery query = new PageQuery();
        query.setSize(50);
        assertEquals(50, query.getSize());
    }

    @Test
    void shouldSetSizeToMinBoundary() {
        PageQuery query = new PageQuery();
        query.setSize(1);
        assertEquals(1, query.getSize());
    }

    @Test
    void shouldSetSizeToMaxBoundary() {
        PageQuery query = new PageQuery();
        query.setSize(500);
        assertEquals(500, query.getSize());
    }

    @Test
    void shouldSetAndGetSort() {
        PageQuery query = new PageQuery();
        List<String> sort = List.of("name,asc", "createdAt,desc");
        query.setSort(sort);
        assertEquals(sort, query.getSort());
    }

    @Test
    void shouldValidateDefaults() {
        PageQuery query = new PageQuery();
        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectNegativePage() {
        PageQuery query = new PageQuery();
        query.setPage(-1);
        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertFalse(violations.isEmpty());
        Set<String> messages = violationsAsMessages(violations);
        assertTrue(messages.stream().anyMatch(m -> m.contains("0")));
    }

    @Test
    void shouldAcceptPageZero() {
        PageQuery query = new PageQuery();
        query.setPage(0);
        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectSizeAboveMax() {
        PageQuery query = new PageQuery();
        query.setSize(501);
        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertFalse(violations.isEmpty());
        Set<String> messages = violationsAsMessages(violations);
        assertTrue(messages.stream().anyMatch(m -> m.contains("500")));
    }

    @Test
    void shouldAcceptSizeAtMax() {
        PageQuery query = new PageQuery();
        query.setSize(500);
        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldAcceptSortWithMaxSize() {
        PageQuery query = new PageQuery();
        List<String> sort = IntStream.range(0, 10)
                .mapToObj(i -> "field" + i + ",asc")
                .collect(Collectors.toList());
        query.setSort(sort);
        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectSortExceedingMaxSize() {
        PageQuery query = new PageQuery();
        List<String> sort = IntStream.range(0, 11)
                .mapToObj(i -> "field" + i + ",asc")
                .collect(Collectors.toList());
        query.setSort(sort);
        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldAcceptNullSort() {
        PageQuery query = new PageQuery();
        query.setSort(null);
        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertTrue(violations.isEmpty());
    }
}
