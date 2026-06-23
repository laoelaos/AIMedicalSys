package com.aimedical.common.result;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PageQuery {

    @Min(0)
    private int page = 0;

    @Max(500)
    private int size = 20;

    @Size(max = 10)
    private List<String> sort;
}
