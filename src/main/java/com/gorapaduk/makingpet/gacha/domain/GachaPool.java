package com.gorapaduk.makingpet.gacha.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 뽑기 풀 + 확률표. 운영 중 entries 는 수정하지 않는다 (AD-7). */
@Entity
@Table(name = "gacha_pools")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GachaPool extends BaseEntity {

    public enum Type { STANDARD, LIMITED }

    public static final int WEIGHT_TOTAL = 10_000;

    @Id
    private UUID id;

    @Column(nullable = false, length = 40)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    @Column(nullable = false)
    private int singlePrice;

    @Column(nullable = false)
    private int multiPrice;

    @Column(nullable = false)
    private int pityThreshold;

    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "pool", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<GachaPoolEntry> entries = new ArrayList<>();

    public static GachaPool create(String name, Type type, int singlePrice, int multiPrice, int pityThreshold,
                                   LocalDateTime startsAt, LocalDateTime endsAt) {
        GachaPool p = new GachaPool();
        p.id = UUID.randomUUID();
        p.name = name;
        p.type = type;
        p.singlePrice = singlePrice;
        p.multiPrice = multiPrice;
        p.pityThreshold = pityThreshold;
        p.startsAt = startsAt;
        p.endsAt = endsAt;
        p.active = true;
        return p;
    }

    public void addEntry(UUID speciesId, int weight) {
        entries.add(GachaPoolEntry.of(this, speciesId, weight));
    }

    public boolean isOpenAt(LocalDateTime now) {
        return active
                && (startsAt == null || !now.isBefore(startsAt))
                && (endsAt == null || now.isBefore(endsAt));
    }

    public void update(String name, Boolean active, LocalDateTime startsAt, LocalDateTime endsAt) {
        if (name != null) this.name = name;
        if (active != null) this.active = active;
        if (startsAt != null) this.startsAt = startsAt;
        if (endsAt != null) this.endsAt = endsAt;
    }

    public int priceFor(int count) {
        return count == 10 ? multiPrice : singlePrice;
    }
}
