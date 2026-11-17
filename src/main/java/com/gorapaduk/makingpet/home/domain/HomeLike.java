package com.gorapaduk.makingpet.home.domain;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** H-6 좋아요 (1일 1회). 유니크 제약이 중복을 막는다. */
@Entity
@Table(name = "home_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"liker_id", "owner_id", "like_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeLike {

    @Id
    private UUID id;

    @Column(name = "liker_id", nullable = false)
    private UUID likerId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "like_date", nullable = false)
    private LocalDate likeDate;

    public static HomeLike of(UUID likerId, UUID ownerId, LocalDate date) {
        HomeLike l = new HomeLike();
        l.id = UUID.randomUUID();
        l.likerId = likerId;
        l.ownerId = ownerId;
        l.likeDate = date;
        return l;
    }
}
