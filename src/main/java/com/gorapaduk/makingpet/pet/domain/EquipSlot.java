package com.gorapaduk.makingpet.pet.domain;

import com.gorapaduk.makingpet.item.domain.ItemCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 규칙 §3-1. 신발은 ACCESSORY 에 포함 (D-12). */
@Getter
@RequiredArgsConstructor
public enum EquipSlot {
    TOP(ItemCategory.TOP, true),
    BOTTOM(ItemCategory.BOTTOM, true),
    ACCESSORY_1(ItemCategory.ACCESSORY, true),
    ACCESSORY_2(ItemCategory.ACCESSORY, true),
    MOTION(ItemCategory.MOTION, false);

    private final ItemCategory category;
    /** 인벤토리 수량을 예약(reserve)하는 슬롯인지. 움직임은 종 단위 해금이라 예약하지 않는다. */
    private final boolean reserves;
}
