package com.gorapaduk.makingpet.item.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gorapaduk.makingpet.item.domain.InventoryItem;
import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.item.dto.InventoryItemResponse;
import com.gorapaduk.makingpet.item.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 도메인이 쓰는 컨트랙트 (02_architecture §도메인 간 호출):
 *   grant    — 지급 (상점 구매, 경매 구매, 친밀도 보상, 경매 반환)
 *   withdraw — 보유에서 빼기 (경매 등록 에스크로)
 *   reserve / release — 장착·배치 예약
 * 전부 호출자 트랜잭션에 참여하고, 행을 PESSIMISTIC_WRITE 로 잠근 뒤 바꾼다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryItemRepository inventory;
    private final ItemService itemService;

    @Transactional(propagation = Propagation.REQUIRED)
    public InventoryItem grant(UUID memberId, UUID itemId, int amount) {
        InventoryItem row = lockOrCreate(memberId, itemId);
        row.add(amount);
        return row;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public InventoryItem withdraw(UUID memberId, UUID itemId, int amount) {
        InventoryItem row = lockOrCreate(memberId, itemId);
        row.withdraw(amount);
        return row;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public InventoryItem reserve(UUID memberId, UUID itemId, int amount) {
        InventoryItem row = lockOrCreate(memberId, itemId);
        row.reserve(amount);
        return row;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public InventoryItem release(UUID memberId, UUID itemId, int amount) {
        InventoryItem row = lockOrCreate(memberId, itemId);
        row.release(amount);
        return row;
    }

    public int available(UUID memberId, UUID itemId) {
        return inventory.findById(new com.gorapaduk.makingpet.item.domain.InventoryItemId(memberId, itemId))
                .map(InventoryItem::getAvailable).orElse(0);
    }

    /** 여러 아이템의 보유 수량(quantity). 없는 아이템은 맵에 없다. 상점 목록의 owned 등에 쓴다. */
    public Map<UUID, Integer> quantities(UUID memberId, Collection<UUID> itemIds) {
        if (itemIds.isEmpty()) return Map.of();
        return inventory.findByMemberAndItems(memberId, itemIds).stream()
                .collect(Collectors.toMap(InventoryItem::getItemId, InventoryItem::getQuantity, (a, b) -> a));
    }

    /** I-2 */
    public List<InventoryItemResponse> list(UUID memberId, ItemCategory category) {
        List<InventoryItem> rows = inventory.findOwned(memberId);
        Map<UUID, Item> items = itemService.getMap(rows.stream().map(InventoryItem::getItemId).toList());
        return rows.stream()
                .filter(row -> items.containsKey(row.getItemId()))
                .filter(row -> category == null || items.get(row.getItemId()).getCategory() == category)
                .map(row -> InventoryItemResponse.of(items.get(row.getItemId()), row))
                .sorted(Comparator.comparing((InventoryItemResponse r) -> r.item().category())
                        .thenComparing(r -> r.item().name()))
                .collect(Collectors.toList());
    }

    /**
     * 행을 잠그고 없으면 만든다. 같은 (member, item) 의 첫 지급이 동시에 두 번 오면
     * 한쪽이 PK 충돌로 실패하므로 한 번 재시도한다.
     */
    private InventoryItem lockOrCreate(UUID memberId, UUID itemId) {
        return inventory.findForUpdate(memberId, itemId).orElseGet(() -> {
            itemService.getById(itemId);                        // 존재하지 않는 아이템이면 ITEM_NOT_FOUND
            try {
                return inventory.saveAndFlush(InventoryItem.empty(memberId, itemId));
            } catch (DataIntegrityViolationException race) {
                return inventory.findForUpdate(memberId, itemId).orElseThrow();
            }
        });
    }
}
