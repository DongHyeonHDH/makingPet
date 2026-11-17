package com.gorapaduk.makingpet.item.repository;

import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** QueryDSL 로 구현하는 동적 조회. 구현체는 {@link ItemRepositoryImpl}. */
public interface ItemRepositoryCustom {

    /** I-1 판매 중(active) 카탈로그. category 가 null 이면 전체. 정렬은 category, name 고정. */
    Page<Item> searchActive(ItemCategory category, Pageable pageable);
}
