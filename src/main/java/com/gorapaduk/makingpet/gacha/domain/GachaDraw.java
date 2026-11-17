package com.gorapaduk.makingpet.gacha.domain;

import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import com.gorapaduk.makingpet.common.constants.Rarity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 뽑기 회차 기록 (확률 검증·CS 용). */
@Entity
@Table(name = "gacha_draws", indexes = @Index(name = "idx_gacha_draws_member_created", columnList = "member_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GachaDraw extends BaseEntity {

    public enum ResultType { NEW_PET, REFUND }

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "pool_id", nullable = false)
    private UUID poolId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(nullable = false)
    private int drawOrder;

    @Column(name = "species_id", nullable = false)
    private UUID speciesId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Rarity rarity;

    @Column(nullable = false)
    private boolean pityTriggered;

    @Column(nullable = false)
    private boolean guaranteed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultType resultType;

    private UUID userPetId;

    @Column(nullable = false)
    private int refundCoins;

    @Column(nullable = false)
    private int cost;

    public static GachaDraw of(UUID memberId, UUID poolId, UUID batchId, int order, UUID speciesId, Rarity rarity,
                               boolean pity, boolean guaranteed, ResultType resultType, UUID userPetId, int refund, int cost) {
        GachaDraw d = new GachaDraw();
        d.id = UUID.randomUUID();
        d.memberId = memberId;
        d.poolId = poolId;
        d.batchId = batchId;
        d.drawOrder = order;
        d.speciesId = speciesId;
        d.rarity = rarity;
        d.pityTriggered = pity;
        d.guaranteed = guaranteed;
        d.resultType = resultType;
        d.userPetId = userPetId;
        d.refundCoins = refund;
        d.cost = cost;
        return d;
    }
}
