package com.gorapaduk.makingpet.shop.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상점 상품 (03_erd `shop_products`, 규칙 §6). 재고는 무한, 가격은 고정.
 * 판매 기간은 KST 기준 LocalDateTime. 시작·끝이 null 이면 그쪽으로 열려 있다. 끝 시각은 포함한다.
 * item 은 다른 도메인이라 ID 만 보관한다.
 */
@Entity
@Table(name = "shop_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopProduct extends BaseEntity {

    public static final int MIN_PRICE = 1;
    public static final int MAX_PRICE = 99_999;

    @Id
    private UUID id;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private int price;

    private LocalDateTime saleStartsAt;

    private LocalDateTime saleEndsAt;

    @Column(nullable = false)
    private boolean visible;

    @Column(nullable = false)
    private int sortOrder;

    public static ShopProduct create(UUID itemId, int price, LocalDateTime saleStartsAt, LocalDateTime saleEndsAt,
                                     boolean visible, int sortOrder) {
        ShopProduct p = new ShopProduct();
        p.id = UUID.randomUUID();
        p.itemId = itemId;
        p.price = price;
        p.saleStartsAt = saleStartsAt;
        p.saleEndsAt = saleEndsAt;
        p.visible = visible;
        p.sortOrder = sortOrder;
        return p;
    }

    /** AD-9. null 은 "보내지 않음" 으로 본다. */
    public void update(Integer price, LocalDateTime saleStartsAt, LocalDateTime saleEndsAt, Boolean visible, Integer sortOrder) {
        if (price != null) this.price = price;
        if (saleStartsAt != null) this.saleStartsAt = saleStartsAt;
        if (saleEndsAt != null) this.saleEndsAt = saleEndsAt;
        if (visible != null) this.visible = visible;
        if (sortOrder != null) this.sortOrder = sortOrder;
    }

    /** 노출 중이고 기간 안인가. 아이템 active 여부는 서비스가 함께 본다. */
    public boolean isOnSale(LocalDateTime now) {
        return visible && !isDeleted()
                && (saleStartsAt == null || !now.isBefore(saleStartsAt))
                && (saleEndsAt == null || !now.isAfter(saleEndsAt));
    }
}
