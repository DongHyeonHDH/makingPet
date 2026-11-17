package com.gorapaduk.makingpet.item.domain;

/** 03_erd `items.category`. FRUIT 은 v1.1 진화 재료 (지금은 등록만 허용, 사용처 없음). */
public enum ItemCategory {
    TOP, BOTTOM, ACCESSORY, MOTION, FURNITURE, THEME, FRUIT;

    /** 펫 장착 슬롯에 들어갈 수 있는 카테고리 */
    public boolean isEquippable() {
        return this == TOP || this == BOTTOM || this == ACCESSORY || this == MOTION;
    }
}
