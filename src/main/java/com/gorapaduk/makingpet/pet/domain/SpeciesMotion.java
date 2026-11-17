package com.gorapaduk.makingpet.pet.domain;

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

/** 친밀도 단계별 해금 움직임 (species_motions). motionItemId 는 item 도메인의 MOTION 아이템 ID. */
@Entity
@Table(name = "species_motions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpeciesMotion {

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Key implements Serializable {
        @Column(name = "species_id", nullable = false)
        private UUID speciesId;
        @Column(name = "affection_level", nullable = false)
        private int affectionLevel;
    }

    @EmbeddedId
    private Key id;

    @Column(name = "motion_item_id", nullable = false)
    private UUID motionItemId;

    public static SpeciesMotion of(UUID speciesId, int affectionLevel, UUID motionItemId) {
        SpeciesMotion m = new SpeciesMotion();
        m.id = new Key(speciesId, affectionLevel);
        m.motionItemId = motionItemId;
        return m;
    }

    public int getAffectionLevel() { return id.getAffectionLevel(); }
}
