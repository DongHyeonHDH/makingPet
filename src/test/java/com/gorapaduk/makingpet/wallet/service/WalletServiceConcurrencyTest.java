package com.gorapaduk.makingpet.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.wallet.domain.TransactionReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 규칙 §4-3: 어떤 동시 요청에서도 잔액이 음수가 되거나 코인이 복제되지 않는다.
 * @Transactional 을 붙이지 않는다 — 스레드마다 실제 커밋이 일어나야 동시성을 검증할 수 있다.
 */
@SpringBootTest
class WalletServiceConcurrencyTest {

    @Autowired WalletService walletService;

    private UUID newWallet(long initial) {
        UUID memberId = UUID.randomUUID();
        walletService.open(memberId);
        if (initial > 0) walletService.credit(memberId, initial, TransactionReason.ADMIN_ADJUST, "TEST", null);
        return memberId;
    }

    @Nested
    @DisplayName("Describe_debit는")
    class Debit {

        @Test
        @DisplayName("Context_잔액이_부족한_경우_It_WALLET_INSUFFICIENT_BALANCE_를_던지고_잔액은_그대로다")
        void insufficient() {
            UUID memberId = newWallet(50);
            assertThatThrownBy(() -> walletService.debit(memberId, 100, TransactionReason.GACHA, "TEST", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
            assertThat(walletService.getBalance(memberId).balance()).isEqualTo(50);
        }

        @Test
        @DisplayName("Context_잔액_100으로_30원씩_20스레드가_동시에_차감하면_It_3번만_성공하고_잔액은_10이다")
        void concurrentDebit() throws Exception {
            UUID memberId = newWallet(100);
            int threads = 20;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            AtomicInteger success = new AtomicInteger();
            AtomicInteger insufficient = new AtomicInteger();

            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                        walletService.debit(memberId, 30, TransactionReason.GACHA, "TEST", null);
                        success.incrementAndGet();
                    } catch (BusinessException e) {
                        if (e.getErrorCode() == ErrorCode.WALLET_INSUFFICIENT_BALANCE) insufficient.incrementAndGet();
                    } catch (InterruptedException ignored) {
                    }
                });
            }
            ready.await();
            go.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

            assertThat(success.get()).isEqualTo(3);
            assertThat(insufficient.get()).isEqualTo(17);
            assertThat(walletService.getBalance(memberId).balance()).isEqualTo(10);
            assertThat(walletService.listTransactions(memberId, TransactionReason.GACHA, 0, 50).totalElements()).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("Describe_credit는_It_원장에_balanceAfter_를_남긴다")
    void creditLedger() {
        UUID memberId = newWallet(0);
        walletService.credit(memberId, 30, TransactionReason.ATTENDANCE, "TEST", null);
        walletService.credit(memberId, 50, TransactionReason.RPS, "TEST", null);
        var page = walletService.listTransactions(memberId, null, 0, 10);
        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0).reason()).isEqualTo(TransactionReason.RPS);     // 최신순
        assertThat(page.items().get(0).balanceAfter()).isEqualTo(80);
        assertThat(page.items().get(1).balanceAfter()).isEqualTo(30);
    }
}
