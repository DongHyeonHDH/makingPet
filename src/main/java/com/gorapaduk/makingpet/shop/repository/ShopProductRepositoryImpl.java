package com.gorapaduk.makingpet.shop.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.item.domain.QItem;
import com.gorapaduk.makingpet.shop.domain.QShopProduct;
import com.gorapaduk.makingpet.shop.domain.ShopProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * 상품은 item 을 ID 로만 들고 있어 연관이 없다. 카테고리·active 필터는 items 와 ID 로 조인한다(조회 전용).
 */
@RequiredArgsConstructor
public class ShopProductRepositoryImpl implements ShopProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ShopProduct> searchOnSale(LocalDateTime now, ItemCategory category, Pageable pageable) {
        QShopProduct p = QShopProduct.shopProduct;
        QItem i = QItem.item;
        BooleanBuilder where = new BooleanBuilder()
                .and(p.deletedAt.isNull())
                .and(p.visible.isTrue())
                .and(p.saleStartsAt.isNull().or(p.saleStartsAt.loe(now)))
                .and(p.saleEndsAt.isNull().or(p.saleEndsAt.goe(now)))
                .and(i.active.isTrue());
        if (category != null) where.and(i.category.eq(category));

        List<ShopProduct> content = queryFactory.select(p)
                .from(p).join(i).on(i.id.eq(p.itemId))
                .where(where)
                .orderBy(p.sortOrder.asc(), p.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long total = queryFactory.select(p.count())
                .from(p).join(i).on(i.id.eq(p.itemId))
                .where(where)
                .fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
