package com.aimedical.common.result;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public PageResponse() {
    }

    public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
        PageResponse<T> response = new PageResponse<>();
        response.content = content;
        response.totalElements = totalElements;
        response.page = page;
        response.size = size;
        response.totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return response;
    }
}
