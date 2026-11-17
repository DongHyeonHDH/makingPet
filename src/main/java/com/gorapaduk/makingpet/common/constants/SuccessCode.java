package com.gorapaduk.makingpet.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 성공 응답의 message. 도메인별 섹션, append-only. */
@Getter
@RequiredArgsConstructor
public enum SuccessCode {

    // ===== Common =====
    OK("요청이 처리되었습니다."),
    READ("조회되었습니다."),

    // ===== Member =====
    MEMBER_CREATED("가입을 환영합니다."),
    MEMBER_LOGIN("로그인되었습니다."),
    MEMBER_LOGOUT("로그아웃되었습니다."),
    MEMBER_UPDATED("정보가 수정되었습니다."),
    MEMBER_DELETED("탈퇴가 완료되었습니다."),

    // ===== Pet =====
    PET_UPDATED("펫 정보가 수정되었습니다."),
    PET_INTERACTED("펫이 좋아합니다!"),
    PET_RELEASED("펫을 놓아주었습니다."),

    // ===== Home =====
    HOME_UPDATED("마이 홈이 수정되었습니다."),

    // ===== Gacha =====
    GACHA_NEW_PET("새 친구를 만났습니다!"),
    GACHA_REFUND_ONLY("이미 있는 친구들이네요. 코인으로 돌려받았습니다."),
    ;

    private final String message;
}
