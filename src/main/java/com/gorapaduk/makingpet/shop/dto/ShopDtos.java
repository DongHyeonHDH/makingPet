package com.gorapaduk.makingpet.shop.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gorapaduk.makingpet.item.dto.ItemSummary;
import com.gorapaduk.makingpet.wallet.dto.WalletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 기획/04_api/08_명세서_Shop.md + 10_명세서_Community_Admin.md AD-8 · AD-9 */
public final class ShopDtos {

    private ShopDtos() {}

    /** S-1 · S-2. owned 는 로그인한 경우 보유 수량, 익명이면 null (명세상 키는 항상 내려준다). */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Product(UUID id, ItemSummary item, int price, LocalDateTime saleEndsAt, Integer owned) {}

    /** S-3 요청. quantity 를 생략하면 1. */
    public record PurchaseRequest(@Min(1) @Max(10) Integer quantity) {
        public int quantityOrDefault() {
            return quantity == null ? 1 : quantity;
        }
    }

    /** S-3 응답 */
    public record PurchaseResponse(UUID purchaseId, ItemSummary item, int quantity, int unitPrice, int totalPrice,
                                   InventorySnapshot inventory, WalletResponse wallet) {}

    public record InventorySnapshot(int quantity, int available) {}

    // ----- Admin -----

    /** AD-8 */
    public record CreateRequest(
            @NotNull UUID itemId,
            @NotNull @Min(1) @Max(99_999) Integer price,
            OffsetDateTime saleStartsAt,
            OffsetDateTime saleEndsAt,
            Boolean visible,
            Integer sortOrder) {}

    /** AD-9. 보낸 필드만 바뀐다. */
    public record UpdateRequest(
            @Min(1) @Max(99_999) Integer price,
            OffsetDateTime saleStartsAt,
            OffsetDateTime saleEndsAt,
            Boolean visible,
            Integer sortOrder) {}

    /** 관리자 응답: 판매 조건 전체 */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record AdminProduct(UUID id, ItemSummary item, int price, LocalDateTime saleStartsAt, LocalDateTime saleEndsAt,
                               boolean visible, int sortOrder, boolean onSale) {}
}
