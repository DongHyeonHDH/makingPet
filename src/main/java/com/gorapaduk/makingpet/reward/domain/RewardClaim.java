package com.gorapaduk.makingpet.reward.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 출석·가위바위보·슬롯 참여 기록.
 * ATTENDANCE / RPS 는 (member, type, date) 유니크로 하루 1회를 보장한다. SLOT 은 횟수가 여러 번이라 유니크 대상이 아니다
 * (slotSeq 로 구분, DailyCounter 가 한도를 관리).
 */
@Entity
@Table(name = "reward_claims",
        uniqueConstraints = @UniqueConstraint(name = "uk_reward_claims_member_type_date_seq",
                columnNames = {"member_id", "type", "reward_date", "seq"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardClaim extends BaseEntity {

    public enum Type { ATTENDANCE, RPS, SLOT }

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Type type;

    @Column(name = "reward_date", nullable = false)
    private LocalDate rewardDate;

    /** 하루 1회 유니크용 키: ATTENDANCE/RPS 는 0, SLOT 은 1..N */
    @Column(name = "seq", nullable = false)
    private int seq;

    /** ATTENDANCE: 연속일 */
    @Column(nullable = false)
    private int streak;

    /** RPS: "ROCK>SCISSORS:WIN", SLOT: "STAR,STAR,STAR:JACKPOT" */
    @Column(length = 100)
    private String detail;

    @Column(nullable = false)
    private int coinDelta;

    public static RewardClaim of(UUID memberId, Type type, LocalDate date, int seq, int streak, String detail, int coinDelta) {
        RewardClaim c = new RewardClaim();
        c.id = UUID.randomUUID();
        c.memberId = memberId;
        c.type = type;
        c.rewardDate = date;
        c.seq = seq;
        c.streak = streak;
        c.detail = detail;
        c.coinDelta = coinDelta;
        return c;
    }
}
