package com.gorapaduk.makingpet.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 펫·아이템 공용 희귀도. 중복 펫 환급 코인은 규칙 문서 §1-5. */
@Getter
@RequiredArgsConstructor
public enum Rarity {
    COMMON(20), RARE(50), EPIC(150), LEGENDARY(500);

    /** 이미 보유한 종이 뽑혔을 때 환급하는 코인 */
    private final int refundCoins;
}
