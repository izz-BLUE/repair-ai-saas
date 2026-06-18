package com.repair.ai.saas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private long total;
    private int page;
    private int size;
    private List<T> records;

    public static <T> PageResponse<T> of(long total, int page, int size, List<T> records) {
        return new PageResponse<>(total, page, size, records);
    }
}
