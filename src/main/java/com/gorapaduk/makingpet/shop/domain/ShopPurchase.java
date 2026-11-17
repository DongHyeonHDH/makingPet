package com.gorapaduk.makingpet.shop.domain;

import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 구매 기록. 가격은 구매 시점 스냅샷 (상품 가격이 나중에 바뀌어도 기록은 유지). */
@Entity
@Table(name = "shop_purchases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopPurchase extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private int totalPrice;

    public static ShopPurchase of(UUID id, UUID memberId, ShopProduct product, int quantity) {
        ShopPurchase p = new ShopPurchase();
        p.id = id;
        p.memberId = memberId;
        p.productId = product.getId();
        p.itemId = product.getItemId();
        p.quantity = quantity;
        p.unitPrice = product.getPrice();
        p.totalPrice = product.getPrice() * quantity;
        return p;
    }
}
