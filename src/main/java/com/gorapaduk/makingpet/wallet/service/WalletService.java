package com.gorapaduk.makingpet.wallet.service;

import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.dto.PageResponse;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.common.util.PageRequests;
import com.gorapaduk.makingpet.wallet.domain.CoinTransaction;
import com.gorapaduk.makingpet.wallet.domain.TransactionReason;
import com.gorapaduk.makingpet.wallet.domain.Wallet;
import com.gorapaduk.makingpet.wallet.dto.TransactionResponse;
import com.gorapaduk.makingpet.wallet.dto.WalletResponse;
import com.gorapaduk.makingpet.wallet.repository.CoinTransactionRepository;
import com.gorapaduk.makingpet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 도메인이 쓰는 컨트랙트: open / credit / debit / getBalance.
 * credit·debit 은 호출자의 트랜잭션에 참여한다(REQUIRED). 코인 이동과 그 원인(뽑기·구매)이 함께 커밋/롤백되도록.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository wallets;
    private final CoinTransactionRepository transactions;

    /** 가입 시 1회. 이미 있으면 그대로 둔다. */
    @Transactional
    public void open(UUID memberId) {
        if (!wallets.existsById(memberId)) {
            wallets.save(Wallet.open(memberId));
        }
    }

    /** 입금. amount > 0. */
    @Transactional(propagation = Propagation.REQUIRED)
    public WalletResponse credit(UUID memberId, long amount, TransactionReason reason, String refType, UUID refId, String memo) {
        if (amount <= 0) throw new IllegalArgumentException("credit amount must be positive: " + amount);
        if (wallets.credit(memberId, amount) == 0) throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        long after = wallets.findBalance(memberId);
        transactions.save(CoinTransaction.record(memberId, amount, after, reason, refType, refId, memo));
        return new WalletResponse(after);
    }

    /** 오버로드에도 쓰기 트랜잭션을 건다. 안 걸면 클래스 레벨 readOnly 가 적용되고, 내부 호출은 프록시를 거치지 않는다. */
    @Transactional(propagation = Propagation.REQUIRED)
    public WalletResponse credit(UUID memberId, long amount, TransactionReason reason, String refType, UUID refId) {
        return credit(memberId, amount, reason, refType, refId, null);
    }

    /** 출금. 잔액이 부족하면 WALLET_INSUFFICIENT_BALANCE. 조건부 UPDATE 라 동시 요청에도 음수가 되지 않는다. */
    @Transactional(propagation = Propagation.REQUIRED)
    public WalletResponse debit(UUID memberId, long amount, TransactionReason reason, String refType, UUID refId, String memo) {
        if (amount <= 0) throw new IllegalArgumentException("debit amount must be positive: " + amount);
        if (wallets.debitIfEnough(memberId, amount) == 0) {
            throw new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }
        long after = wallets.findBalance(memberId);
        transactions.save(CoinTransaction.record(memberId, -amount, after, reason, refType, refId, memo));
        return new WalletResponse(after);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public WalletResponse debit(UUID memberId, long amount, TransactionReason reason, String refType, UUID refId) {
        return debit(memberId, amount, reason, refType, refId, null);
    }

    /** W-1 */
    public WalletResponse getBalance(UUID memberId) {
        Long balance = wallets.findBalance(memberId);
        return new WalletResponse(balance == null ? 0 : balance);
    }

    /** W-2 (최신순 고정) */
    public PageResponse<TransactionResponse> listTransactions(UUID memberId, TransactionReason reason, int page, int size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = reason == null
                ? transactions.findByMemberId(memberId, pageable)
                : transactions.findByMemberIdAndReason(memberId, reason, pageable);
        return PageResponse.of(result, TransactionResponse::from);
    }
}
