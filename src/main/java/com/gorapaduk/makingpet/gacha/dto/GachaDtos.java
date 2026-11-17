package com.gorapaduk.makingpet.gacha.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gorapaduk.makingpet.common.constants.Rarity;
import com.gorapaduk.makingpet.gacha.domain.GachaDraw;
import com.gorapaduk.makingpet.pet.dto.PetSummary;
import com.gorapaduk.makingpet.pet.dto.SpeciesSummary;
import com.gorapaduk.makingpet.wallet.dto.WalletResponse;
import jakarta.validation.constraints.NotNull;

public final class GachaDtos {

    private GachaDtos() {}

    /** G-1 풀 1건 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PoolResponse(UUID id, String name, String type, int singlePrice, int multiPrice, Rarity multiGuarantee,
                               int pityThreshold, Rarity pityGuarantee, LocalDateTime startsAt, LocalDateTime endsAt,
                               List<RarityRate> rates) {}

    public record RarityRate(Rarity rarity, double rate, List<SpeciesRate> species) {}

    public record SpeciesRate(UUID id, String code, String name, double rate) {}

    /** G-2 요청 */
    public record DrawRequest(@NotNull Integer count) {}

    /** G-2 응답 */
    public record DrawResponse(UUID batchId, UUID poolId, int cost, List<DrawResult> results, Summary summary,
                               Pity pity, WalletResponse wallet) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DrawResult(int order, GachaDraw.ResultType resultType, SpeciesSummary species, PetSummary pet,
                             int refundCoins, boolean pityTriggered, boolean guaranteed) {}

    public record Summary(int newPets, int refunds, int refundCoins) {}

    public record Pity(int count, int threshold) {}

    /** G-3 */
    public record DrawRecord(UUID id, UUID batchId, String poolName, SpeciesSummary species,
                             GachaDraw.ResultType resultType, int refundCoins, boolean pityTriggered, LocalDateTime createdAt) {}

    /** G-4 */
    public record PityResponse(UUID poolId, int count, int threshold, Rarity guarantee) {}
}
