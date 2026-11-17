package com.gorapaduk.makingpet.wallet.domain;

import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

/**
 * 회원당 1개. balance 는 이 엔티티의 setter 로 바꾸지 않고
 * WalletRepository 의 조건부 UPDATE 로만 바꾼다 (규칙 §4-3).
 */
@Entity
@Table(name = "wallets")
@Check(constraints = "balance >= 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseEntity {

    @Id
    @Column(name = "member_id")
    private UUID memberId;

    @Column(nullable = false)
    private long balance;

    public static Wallet open(UUID memberId) {
        Wallet wallet = new Wallet();
        wallet.memberId = memberId;
        wallet.balance = 0;
        return wallet;
    }
}
