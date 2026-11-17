package com.gorapaduk.makingpet.wallet.dto;

/** W-1 응답이자 코인이 움직인 모든 응답에 붙는 WalletSnapshot (01_API_표준 §11). */
public record WalletResponse(long balance) {
}
