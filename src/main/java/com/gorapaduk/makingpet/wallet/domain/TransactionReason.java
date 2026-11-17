package com.gorapaduk.makingpet.wallet.domain;

/** 원장 사유. 03_erd `coin_transactions.reason` 과 1:1. */
public enum TransactionReason {
    SIGNUP_GIFT,
    ATTENDANCE,
    RPS,
    SLOT_ENTRY,
    SLOT_PAYOUT,
    GACHA,
    GACHA_REFUND,
    AFFECTION_REWARD,
    SHOP_PURCHASE,
    AUCTION_BUY,
    AUCTION_SELL,
    AUCTION_FEE,
    ADMIN_ADJUST
}
