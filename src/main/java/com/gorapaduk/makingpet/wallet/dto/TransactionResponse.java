package com.gorapaduk.makingpet.wallet.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gorapaduk.makingpet.wallet.domain.CoinTransaction;
import com.gorapaduk.makingpet.wallet.domain.TransactionReason;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(UUID id, long amount, long balanceAfter, TransactionReason reason,
                                  String refType, UUID refId, String memo, LocalDateTime createdAt) {

    public static TransactionResponse from(CoinTransaction tx) {
        return new TransactionResponse(tx.getId(), tx.getAmount(), tx.getBalanceAfter(), tx.getReason(),
                tx.getRefType(), tx.getRefId(), tx.getMemo(), tx.getCreatedAt());
    }
}
