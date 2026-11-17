package com.gorapaduk.makingpet.reward.domain;

import java.io.Serializable;
import java.time.LocalDate;
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

/** 일일 횟수 카운터 (슬롯). 행을 잠그고 증가시킨다. */
@Entity
@Table(name = "daily_counters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyCounter {

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Key implements Serializable {
        @Column(name = "member_id", nullable = false) private UUID memberId;
        @Column(name = "counter_date", nullable = false) private LocalDate date;
        @Column(name = "type", nullable = false, length = 12) private String type;
    }

    @EmbeddedId
    private Key id;

    @Column(nullable = false)
    private int count;

    public static DailyCounter zero(UUID memberId, LocalDate date, String type) {
        DailyCounter c = new DailyCounter();
        c.id = new Key(memberId, date, type);
        c.count = 0;
        return c;
    }

    public void increment() { count++; }
}
