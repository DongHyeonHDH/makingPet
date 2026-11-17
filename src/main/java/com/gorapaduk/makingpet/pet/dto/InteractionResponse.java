package com.gorapaduk.makingpet.pet.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gorapaduk.makingpet.item.dto.ItemSummary;
import com.gorapaduk.makingpet.wallet.dto.WalletResponse;

/** P-5 응답 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InteractionResponse(
        PetSummary pet,
        String motionKey,
        int moodDelta,
        int affectionDelta,
        LevelUp levelUp,
        LocalDateTime nextAvailableAt,
        WalletResponse wallet) {

    public record LevelUp(int from, int to, Rewards rewards) {}

    public record Rewards(List<ItemSummary> motions, int coins) {}
}
