package com.aimedical.common.result;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    void shouldCreateEmptyPageResponse() {
        PageResponse<String> response = PageResponse.of(List.of(), 0, 0, 20);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());
    }

    @Test
    void shouldCalculateTotalPagesCorrectly() {
        List<String> items = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        PageResponse<String> response = PageResponse.of(items, 10, 0, 3);
        assertEquals(10, response.getTotalElements());
        assertEquals(4, response.getTotalPages());
    }

    @Test
    void shouldReturnSinglePageWhenSizeEqualsTotal() {
        List<String> items = List.of("a", "b", "c");
        PageResponse<String> response = PageResponse.of(items, 3, 0, 3);
        assertEquals(1, response.getTotalPages());
    }

    @Test
    void shouldReturnZeroPagesWhenSizeIsZero() {
        PageResponse<String> response = PageResponse.of(List.of(), 0, 0, 0);
        assertEquals(0, response.getTotalPages());
    }

    @Test
    void shouldReturnZeroPagesWhenTotalIsZero() {
        PageResponse<String> response = PageResponse.of(List.of(), 0, 0, 20);
        assertEquals(0, response.getTotalPages());
    }

    @Test
    void shouldSetAndGetViaSetters() {
        PageResponse<String> response = new PageResponse<>();
        response.setContent(List.of("x"));
        response.setTotalElements(1);
        response.setTotalPages(1);
        response.setPage(0);
        response.setSize(10);
        assertEquals(List.of("x"), response.getContent());
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
    }
}
