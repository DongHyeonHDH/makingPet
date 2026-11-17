package com.gorapaduk.makingpet.item.domain;

import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

/**
 * 보유 수량 (03_erd `inventory_items`).
 * quantity: 보유 (경매 에스크로분은 이미 빠진 값) · reserved: 장착·배치 중.
 * 거래 가능 수량(available) = quantity − reserved.
 */
@Entity
@Table(name = "inventory_items")
@Check(constraints = "quantity >= 0 and reserved >= 0 and reserved <= quantity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryItem extends BaseEntity {

    @EmbeddedId
    private InventoryItemId id;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reserved;

    public static InventoryItem empty(UUID memberId, UUID itemId) {
        InventoryItem row = new InventoryItem();
        row.id = new InventoryItemId(memberId, itemId);
        return row;
    }

    public UUID getItemId() { return id.getItemId(); }

    public int getAvailable() { return quantity - reserved; }

    public void add(int amount) {
        this.quantity += amount;
    }

    /** 경매 등록 등으로 보유에서 빼기 (에스크로). 거래 가능 수량만 뺄 수 있다. */
    public void withdraw(int amount) {
        if (getAvailable() < amount) throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT);
        this.quantity -= amount;
    }

    /** 장착·배치: 거래 가능 수량을 예약으로 돌린다. */
    public void reserve(int amount) {
        if (getAvailable() < amount) throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT);
        this.reserved += amount;
    }

    /** 장착 해제·배치 해제 */
    public void release(int amount) {
        if (reserved < amount) throw new IllegalStateException("reserved 보다 많이 release: " + id.getItemId());
        this.reserved -= amount;
    }
}
