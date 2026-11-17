package com.gorapaduk.makingpet.gacha.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 풀 × 종 가중치 (만분율, 풀 합계 10000). speciesId 는 pet 도메인 ID 만 보관. */
@Entity
@Table(name = "gacha_pool_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pool_id", "species_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GachaPoolEntry {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private GachaPool pool;

    @Column(name = "species_id", nullable = false)
    private UUID speciesId;

    @Column(nullable = false)
    private int weight;

    static GachaPoolEntry of(GachaPool pool, UUID speciesId, int weight) {
        GachaPoolEntry e = new GachaPoolEntry();
        e.id = UUID.randomUUID();
        e.pool = pool;
        e.speciesId = speciesId;
        e.weight = weight;
        return e;
    }
}
