package com.gorapaduk.makingpet.gacha.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 풀별 천장 카운터: 마지막 EPIC 이상 이후 횟수. */
@Entity
@Table(name = "gacha_pity_counters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GachaPityCounter {

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Key implements Serializable {
        @Column(name = "member_id", nullable = false) private UUID memberId;
        @Column(name = "pool_id", nullable = false) private UUID poolId;
    }

    @EmbeddedId
    private Key id;

    @Column(nullable = false)
    private int count;

    public static GachaPityCounter zero(UUID memberId, UUID poolId) {
        GachaPityCounter c = new GachaPityCounter();
        c.id = new Key(memberId, poolId);
        c.count = 0;
        return c;
    }

    public void increment() { count++; }

    public void reset() { count = 0; }
}
