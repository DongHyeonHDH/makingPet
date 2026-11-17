package com.gorapaduk.makingpet.wallet.repository;

import java.util.UUID;

import com.gorapaduk.makingpet.wallet.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /** 조건부 차감. 영향받은 행이 0이면 잔액 부족. 동시 요청에도 음수가 되지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Wallet w set w.balance = w.balance - :amount, w.updatedAt = CURRENT_TIMESTAMP "
            + "where w.memberId = :memberId and w.balance >= :amount")
    int debitIfEnough(@Param("memberId") UUID memberId, @Param("amount") long amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Wallet w set w.balance = w.balance + :amount, w.updatedAt = CURRENT_TIMESTAMP "
            + "where w.memberId = :memberId")
    int credit(@Param("memberId") UUID memberId, @Param("amount") long amount);

    @Query("select w.balance from Wallet w where w.memberId = :memberId")
    Long findBalance(@Param("memberId") UUID memberId);
}
