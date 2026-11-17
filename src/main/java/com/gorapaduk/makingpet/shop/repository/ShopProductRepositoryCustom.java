package com.gorapaduk.makingpet.shop.repository;

import java.time.LocalDateTime;

import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.shop.domain.ShopProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** QueryDSL 동적 조회. 구현체는 {@link ShopProductRepositoryImpl}. */
public interface ShopProductRepositoryCustom {

    /** S-1 지금 판매 중(visible · 기간 안 · 아이템 active)인 상품. category 가 null 이면 전체. 정렬 sort_order, id. */
    Page<ShopProduct> searchOnSale(LocalDateTime now, ItemCategory category, Pageable pageable);
}
