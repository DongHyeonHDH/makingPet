package com.gorapaduk.makingpet.item.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gorapaduk.makingpet.common.constants.Rarity;
import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.domain.ItemCategory;

/** 01_API_표준 §11 ItemSummary. description 은 상세에서만 채운다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemSummary(UUID id, String code, String name, ItemCategory category, Rarity rarity,
                          String assetKey, boolean tradable, String description) {

    public static ItemSummary from(Item item) {
        return new ItemSummary(item.getId(), item.getCode(), item.getName(), item.getCategory(), item.getRarity(),
                item.getAssetKey(), item.isTradable(), null);
    }

    public static ItemSummary detail(Item item) {
        return new ItemSummary(item.getId(), item.getCode(), item.getName(), item.getCategory(), item.getRarity(),
                item.getAssetKey(), item.isTradable(), item.getDescription());
    }
}
