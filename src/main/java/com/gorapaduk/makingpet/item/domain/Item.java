package com.gorapaduk.makingpet.item.domain;

import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import com.gorapaduk.makingpet.common.constants.Rarity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 아이템 카탈로그. code / category / rarity 는 생성 후 바꾸지 않는다 (AD-5). */
@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseEntity {

    public static final String DEFAULT_THEME_CODE = "theme_basic";
    public static final String DEFAULT_MOTION_CODE = "motion_idle";

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 40)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Rarity rarity;

    @Column(nullable = false, length = 200)
    private String assetKey;

    @Column(length = 300)
    private String description;

    @Column(nullable = false)
    private boolean tradable;

    @Column(nullable = false)
    private boolean active;

    public static Item create(String code, String name, ItemCategory category, Rarity rarity,
                              String assetKey, String description, boolean tradable) {
        Item item = new Item();
        item.id = UUID.randomUUID();
        item.code = code;
        item.name = name;
        item.category = category;
        item.rarity = rarity;
        item.assetKey = assetKey;
        item.description = description;
        item.tradable = category != ItemCategory.MOTION && tradable;   // MOTION 은 거래 불가 강제
        item.active = true;
        return item;
    }

    public void update(String name, String assetKey, String description, Boolean tradable, Boolean active) {
        if (name != null) this.name = name;
        if (assetKey != null) this.assetKey = assetKey;
        if (description != null) this.description = description;
        if (tradable != null) this.tradable = category != ItemCategory.MOTION && tradable;
        if (active != null) this.active = active;
    }
}
