package com.gorapaduk.makingpet.pet.domain;

import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import com.gorapaduk.makingpet.common.constants.Rarity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 펫 종 카탈로그. code · rarity 는 생성 후 불변 (AD-2). */
@Entity
@Table(name = "pet_species")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetSpecies extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 40)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Rarity rarity;

    @Column(nullable = false, length = 200)
    private String baseAssetKey;

    @Column(length = 300)
    private String description;

    @Column(nullable = false)
    private boolean active;

    public static PetSpecies create(String code, String name, Rarity rarity, String baseAssetKey, String description) {
        PetSpecies s = new PetSpecies();
        s.id = UUID.randomUUID();
        s.code = code;
        s.name = name;
        s.rarity = rarity;
        s.baseAssetKey = baseAssetKey;
        s.description = description;
        s.active = true;
        return s;
    }

    public void update(String name, String baseAssetKey, String description, Boolean active) {
        if (name != null) this.name = name;
        if (baseAssetKey != null) this.baseAssetKey = baseAssetKey;
        if (description != null) this.description = description;
        if (active != null) this.active = active;
    }

    public String silhouetteAssetKey() {
        return "pets/" + code + "/silhouette.png";
    }
}
