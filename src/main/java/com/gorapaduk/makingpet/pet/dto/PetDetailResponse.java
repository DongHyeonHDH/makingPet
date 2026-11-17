package com.gorapaduk.makingpet.pet.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gorapaduk.makingpet.item.dto.ItemSummary;
import com.gorapaduk.makingpet.pet.domain.EquipSlot;
import com.gorapaduk.makingpet.pet.domain.InteractionType;

/** P-2. PetSummary 필드 + 상세. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PetDetailResponse(
        UUID id,
        String name,
        SpeciesSummary species,
        int mood,
        PetSummary.MoodState moodState,
        int affectionLevel,
        boolean isRepresentative,
        Map<EquipSlot, PetSummary.EquippedItem> equipment,
        int affection,
        Integer nextLevelAffection,
        int stage,
        Map<InteractionType, LocalDateTime> cooldowns,
        List<ItemSummary> unlockedMotions,
        LocalDateTime obtainedAt) {
}
