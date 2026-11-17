package com.gorapaduk.makingpet.item.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gorapaduk.makingpet.common.config.CacheConfig;
import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.dto.PageResponse;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.common.util.PageRequests;
import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.item.dto.ItemSummary;
import com.gorapaduk.makingpet.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 카탈로그 조회. 등록·수정(AD-4, AD-5)은 admin 도메인에서 이 서비스를 호출한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository items;

    /** I-1. 조건은 QueryDSL(ItemRepositoryImpl), 결과는 캐시(CACHE_TYPE=redis 일 때만 실제 저장). */
    @Cacheable(cacheNames = CacheConfig.ITEM_CATALOG, key = "(#category == null ? 'ALL' : #category.name()) + ':' + #page + ':' + #size")
    public PageResponse<ItemSummary> list(ItemCategory category, int page, int size) {
        var pageable = PageRequests.of(page, size, Sort.by("category", "name"));
        return PageResponse.of(items.searchActive(category, pageable), ItemSummary::from);
    }

    /** 다른 도메인 컨트랙트 */
    public Item getById(UUID itemId) {
        return items.findById(itemId).orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }

    public Item getByCode(String code) {
        return items.findByCode(code).orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }

    /** 없을 수 있는 조회. 예외를 던지지 않으므로 호출자의 트랜잭션을 rollback-only 로 만들지 않는다. */
    public Optional<Item> findByCode(String code) {
        return items.findByCode(code);
    }

    public Map<UUID, Item> getMap(Collection<UUID> itemIds) {
        return items.findByIdIn(itemIds).stream().collect(Collectors.toMap(Item::getId, Function.identity()));
    }

    /** AD-4 */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.ITEM_CATALOG, allEntries = true)
    public Item create(String code, String name, ItemCategory category,
                       com.gorapaduk.makingpet.common.constants.Rarity rarity,
                       String assetKey, String description, boolean tradable) {
        if (items.existsByCode(code)) throw new BusinessException(ErrorCode.ITEM_CODE_DUPLICATED);
        return items.save(Item.create(code, name, category, rarity, assetKey, description, tradable));
    }

    /** AD-5 */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.ITEM_CATALOG, allEntries = true)
    public Item update(UUID itemId, String name, String assetKey, String description, Boolean tradable, Boolean active) {
        Item item = getById(itemId);
        item.update(name, assetKey, description, tradable, active);
        return item;
    }

    public List<Item> findAll() {
        return items.findAll();
    }
}
