package com.gorapaduk.makingpet.wallet.repository;

import java.util.UUID;

import com.gorapaduk.makingpet.wallet.domain.CoinTransaction;
import com.gorapaduk.makingpet.wallet.domain.TransactionReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {

    Page<CoinTransaction> findByMemberId(UUID memberId, Pageable pageable);

    Page<CoinTransaction> findByMemberIdAndReason(UUID memberId, TransactionReason reason, Pageable pageable);
}
