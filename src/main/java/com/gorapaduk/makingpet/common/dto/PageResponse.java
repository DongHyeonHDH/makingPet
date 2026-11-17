package com.gorapaduk.makingpet.common.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Spring Page 를 그대로 직렬화하지 않고 이 형식으로 변환한다 (01_API_표준 §1). */
public record PageResponse<T>(List<T> items, int page, int size,
                              long totalElements, int totalPages, boolean first, boolean last) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.map(mapper).getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return of(page, Function.identity());
    }
}
