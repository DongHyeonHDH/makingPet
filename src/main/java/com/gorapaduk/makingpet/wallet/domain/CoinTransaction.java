package com.gorapaduk.makingpet.wallet.domain;

import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 원장 (append-only). amount: +입금 / −출금. */
@Entity
@Table(name = "coin_transactions",
        indexes = @Index(name = "idx_coin_tx_member_created", columnList = "member_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoinTransaction extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private long balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionReason reason;

    @Column(length = 30)
    private String refType;

    private UUID refId;

    @Column(length = 200)
    private String memo;

    public static CoinTransaction record(UUID memberId, long amount, long balanceAfter,
                                         TransactionReason reason, String refType, UUID refId, String memo) {
        CoinTransaction tx = new CoinTransaction();
        tx.id = UUID.randomUUID();
        tx.memberId = memberId;
        tx.amount = amount;
        tx.balanceAfter = balanceAfter;
        tx.reason = reason;
        tx.refType = refType;
        tx.refId = refId;
        tx.memo = memo;
        return tx;
    }
}
