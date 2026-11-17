package com.gorapaduk.makingpet.pet.dto;

import java.util.Map;
import java.util.UUID;

import com.gorapaduk.makingpet.pet.domain.EquipSlot;
import com.gorapaduk.makingpet.pet.domain.InteractionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PetRequests {

    private PetRequests() {}

    /** P-3 */
    public record Rename(@NotBlank @Size(max = 12) String name) {}

    /** P-5 */
    public record Interact(@NotNull InteractionType type) {}

    /**
     * P-6. 슬롯 전체 교체. 키가 빠진 슬롯은 비운 것으로 본다.
     * 예: { "TOP": "uuid", "BOTTOM": null, "ACCESSORY_1": null, "ACCESSORY_2": null, "MOTION": "uuid" }
     */
    public record Equipment(Map<EquipSlot, UUID> slots) {}
}
