package com.gorapaduk.makingpet.pet.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gorapaduk.makingpet.common.constants.Rarity;
import com.gorapaduk.makingpet.pet.domain.PetSpecies;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpeciesSummary(UUID id, String code, String name, Rarity rarity, String assetKey, String description) {

    public static SpeciesSummary from(PetSpecies s) {
        return new SpeciesSummary(s.getId(), s.getCode(), s.getName(), s.getRarity(), s.getBaseAssetKey(), null);
    }

    public static SpeciesSummary detail(PetSpecies s) {
        return new SpeciesSummary(s.getId(), s.getCode(), s.getName(), s.getRarity(), s.getBaseAssetKey(), s.getDescription());
    }

    /** 도감 미보유: 실루엣, 설명 숨김 */
    public static SpeciesSummary silhouette(PetSpecies s) {
        return new SpeciesSummary(s.getId(), s.getCode(), s.getName(), s.getRarity(), s.silhouetteAssetKey(), null);
    }
}
