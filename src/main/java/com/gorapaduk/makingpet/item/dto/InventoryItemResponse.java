package com.gorapaduk.makingpet.item.dto;

import com.gorapaduk.makingpet.item.domain.InventoryItem;
import com.gorapaduk.makingpet.item.domain.Item;

/** I-2 응답 한 줄. listing(경매 등록 중 수량)은 auction 도메인이 생기면 채운다. */
public record InventoryItemResponse(ItemSummary item, int quantity, int reserved, Integer listing, int available) {

    public static InventoryItemResponse of(Item item, InventoryItem row) {
        return new InventoryItemResponse(ItemSummary.from(item), row.getQuantity(), row.getReserved(), null,
                row.getAvailable());
    }
}
