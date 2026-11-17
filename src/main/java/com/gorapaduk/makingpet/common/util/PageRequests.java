package com.gorapaduk.makingpet.common.util;

import java.util.Set;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** 01_API_표준 §6: size 최대 50, sort 는 도메인별 화이트리스트. */
public final class PageRequests {

    public static final int MAX_SIZE = 50;

    private PageRequests() {}

    public static Pageable of(int page, int size, Sort defaultSort) {
        if (size > MAX_SIZE) throw new BusinessException(ErrorCode.PAGINATION_SIZE_TOO_LARGE);
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1), defaultSort);
    }

    /** sort 파라미터 "field,asc|desc" 를 화이트리스트로 검증한다. null/blank 면 기본 정렬. */
    public static Pageable of(int page, int size, String sort, Set<String> allowedKeys, Sort defaultSort) {
        if (sort == null || sort.isBlank()) return of(page, size, defaultSort);
        String[] parts = sort.split(",");
        String key = parts[0].strip();
        if (!allowedKeys.contains(key)) throw new BusinessException(ErrorCode.SORT_KEY_NOT_ALLOWED);
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].strip())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return of(page, size, Sort.by(direction, key));
    }
}
