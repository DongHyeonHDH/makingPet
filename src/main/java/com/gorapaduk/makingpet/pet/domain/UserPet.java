package com.gorapaduk.makingpet.pet.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보유 펫 (03_erd user_pets). 종당 1마리: (member_id, species_id) 유니크 (삭제된 펫 제외는 소프트 딜리트 시 species_id 를 비워서 처리).
 * 장착(pet_equipments)·쿨다운(pet_interaction_cooldowns)은 @ElementCollection 으로 같은 집계에 둔다.
 */
@Entity
@Table(name = "user_pets",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_pets_member_species", columnNames = {"member_id", "species_id"}),
        indexes = @Index(name = "idx_user_pets_member", columnList = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPet extends BaseEntity {

    public static final int MOOD_MAX = 100;
    public static final int MOOD_INITIAL = 70;
    public static final int MOOD_DECAY_PER_HOUR = 2;

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    /** 소프트 딜리트 시 null 로 비워 유니크 제약에서 빠진다. 원래 종은 releasedSpeciesId 에 남긴다. */
    @Column(name = "species_id")
    private UUID speciesId;

    private UUID releasedSpeciesId;

    @Column(nullable = false, length = 12)
    private String name;

    @Column(nullable = false)
    private int mood;

    @Column(nullable = false)
    private LocalDateTime moodUpdatedAt;

    @Column(nullable = false)
    private int affection;

    @Column(nullable = false)
    private int affectionLevel;

    @Column(nullable = false)
    private int stage;

    @Column(nullable = false, length = 20)
    private String obtainedFrom;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pet_equipments", joinColumns = @JoinColumn(name = "pet_id"))
    @MapKeyColumn(name = "slot", length = 12)
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "item_id", nullable = false)
    private Map<EquipSlot, UUID> equipment = new EnumMap<>(EquipSlot.class);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pet_interaction_cooldowns", joinColumns = @JoinColumn(name = "pet_id"))
    @MapKeyColumn(name = "type", length = 10)
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "next_available_at", nullable = false)
    private Map<InteractionType, LocalDateTime> cooldowns = new EnumMap<>(InteractionType.class);

    public static UserPet create(UUID memberId, PetSpecies species, String obtainedFrom) {
        UserPet pet = new UserPet();
        pet.id = UUID.randomUUID();
        pet.memberId = memberId;
        pet.speciesId = species.getId();
        pet.name = species.getName();
        pet.mood = MOOD_INITIAL;
        pet.moodUpdatedAt = LocalDateTime.now();
        pet.affection = 0;
        pet.affectionLevel = 1;
        pet.stage = 1;
        pet.obtainedFrom = obtainedFrom;
        return pet;
    }

    /** 규칙 §2-2: 1시간에 −2, 지연 계산. */
    public int currentMood(LocalDateTime now) {
        long hours = Duration.between(moodUpdatedAt, now).toHours();
        return (int) Math.max(0, mood - hours * MOOD_DECAY_PER_HOUR);
    }

    public LocalDateTime nextAvailable(InteractionType type) {
        return cooldowns.get(type);
    }

    public boolean isOnCooldown(InteractionType type, LocalDateTime now) {
        LocalDateTime next = cooldowns.get(type);
        return next != null && next.isAfter(now);
    }

    /** 상호작용 적용. 단계가 올랐으면 새 단계, 아니면 0 을 돌려준다. */
    public int interact(InteractionType type, LocalDateTime now) {
        this.mood = Math.min(MOOD_MAX, currentMood(now) + type.getMoodDelta());
        this.moodUpdatedAt = now;
        this.affection += type.getAffectionDelta();
        this.cooldowns.put(type, now.plus(type.getCooldown()));
        int newLevel = AffectionLevel.levelFor(affection);
        if (newLevel > affectionLevel) {
            this.affectionLevel = newLevel;
            return newLevel;
        }
        return 0;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void replaceEquipment(Map<EquipSlot, UUID> newEquipment) {
        this.equipment.clear();
        this.equipment.putAll(newEquipment);
    }

    public void release() {
        this.releasedSpeciesId = this.speciesId;
        this.speciesId = null;
        this.equipment.clear();
        softDelete();
    }
}
