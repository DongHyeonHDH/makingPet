package com.gorapaduk.makingpet.pet.dto;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gorapaduk.makingpet.pet.domain.EquipSlot;

/** 01_API_표준 §11 PetSummary. equipment 는 5개 슬롯 전부 키로 두고 비어 있으면 null. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PetSummary(
        UUID id,
        String name,
        SpeciesSummary species,
        int mood,
        MoodState moodState,
        int affectionLevel,
        boolean isRepresentative,
        Map<EquipSlot, EquippedItem> equipment) {

    public enum MoodState {
        SAD, NORMAL, HAPPY;

        public static MoodState of(int mood) {
            return mood < 30 ? SAD : mood < 70 ? NORMAL : HAPPY;
        }
    }

    public record EquippedItem(UUID itemId, String code, String assetKey) {}
}
