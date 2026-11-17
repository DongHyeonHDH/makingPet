package com.gorapaduk.makingpet.item.repository;

import java.util.List;

import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.item.domain.QItem;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Item> searchActive(ItemCategory category, Pageable pageable) {
        QItem item = QItem.item;
        BooleanBuilder where = new BooleanBuilder(item.active.isTrue());
        if (category != null) where.and(item.category.eq(category));

        List<Item> content = queryFactory.selectFrom(item)
                .where(where)
                .orderBy(item.category.asc(), item.name.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long total = queryFactory.select(item.count()).from(item).where(where).fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
