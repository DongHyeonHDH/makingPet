package com.gorapaduk.makingpet.item.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InventoryItemId implements Serializable {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;
}
