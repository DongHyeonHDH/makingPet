package com.gorapaduk.makingpet.home.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 가구 배치 1개. 좌표는 0~100 비율, layer 오름차순으로 그린다 (프로토콜 §3-1). */
@Entity
@Table(name = "home_furnitures", indexes = @Index(name = "idx_home_furnitures_member", columnList = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeFurniture {

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private int posX;

    @Column(nullable = false)
    private int posY;

    @Column(nullable = false)
    private int layer;

    public static HomeFurniture of(UUID memberId, UUID itemId, int x, int y, int layer) {
        HomeFurniture f = new HomeFurniture();
        f.id = UUID.randomUUID();
        f.memberId = memberId;
        f.itemId = itemId;
        f.posX = x;
        f.posY = y;
        f.layer = layer;
        return f;
    }
}
