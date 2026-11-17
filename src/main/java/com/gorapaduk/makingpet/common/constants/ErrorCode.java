package com.gorapaduk.makingpet.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 사전 (기획/04_api/12_에러_코드_사전.md) 와 1:1.
 * 사전에 없는 코드는 추가하지 않는다. 도메인별 섹션은 append-only.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== Common =====
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "검증에 실패했습니다."),
    PAGINATION_SIZE_TOO_LARGE(HttpStatus.BAD_REQUEST, "페이지 크기는 50 이하여야 합니다."),
    SORT_KEY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않은 정렬 기준입니다."),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "Idempotency-Key 헤더가 필요합니다."),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "이미 다른 요청에 사용된 키입니다."),
    TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "액세스 토큰이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "액세스 토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "해당 작업을 수행할 권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    // ===== Member =====
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    MEMBER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입한 이메일입니다."),
    MEMBER_NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    MEMBER_NICKNAME_FORBIDDEN_WORD(HttpStatus.BAD_REQUEST, "사용할 수 없는 닉네임입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    MEMBER_SELF_ROLE_CHANGE(HttpStatus.FORBIDDEN, "본인의 권한은 변경할 수 없습니다."),

    // ===== Pet =====
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "펫을 찾을 수 없습니다."),
    PET_NOT_OWNED(HttpStatus.FORBIDDEN, "내 펫이 아닙니다."),
    PET_NAME_FORBIDDEN_WORD(HttpStatus.BAD_REQUEST, "사용할 수 없는 이름입니다."),
    PET_INTERACTION_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "아직 쉬는 중이에요."),
    PET_MOTION_NOT_UNLOCKED(HttpStatus.BAD_REQUEST, "아직 해금되지 않은 움직임입니다."),
    PET_RELEASE_NOT_ALLOWED(HttpStatus.CONFLICT, "대표 펫이나 마지막 펫은 놓아줄 수 없습니다."),
    PET_SPECIES_NOT_FOUND(HttpStatus.NOT_FOUND, "펫 종을 찾을 수 없습니다."),
    PET_SPECIES_CODE_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 펫 종 코드입니다."),
    PET_SPECIES_INACTIVE(HttpStatus.BAD_REQUEST, "비활성 펫 종입니다."),

    // ===== Gacha =====
    GACHA_POOL_NOT_FOUND(HttpStatus.NOT_FOUND, "뽑기 풀을 찾을 수 없습니다."),
    GACHA_POOL_INACTIVE(HttpStatus.CONFLICT, "지금은 진행 중이지 않은 뽑기입니다."),
    GACHA_WEIGHT_SUM_INVALID(HttpStatus.BAD_REQUEST, "확률 가중치 합계가 올바르지 않습니다."),
    GACHA_POOL_GUARANTEE_IMPOSSIBLE(HttpStatus.BAD_REQUEST, "보장·천장을 만족할 수 있는 종이 없습니다."),
    GACHA_POOL_ENTRIES_IMMUTABLE(HttpStatus.CONFLICT, "운영 중인 풀의 확률은 수정할 수 없습니다."),

    // ===== Item / Inventory / Home =====
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다."),
    ITEM_CODE_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 아이템 코드입니다."),
    ITEM_SLOT_MISMATCH(HttpStatus.BAD_REQUEST, "해당 슬롯에 장착할 수 없는 아이템입니다."),
    ITEM_NOT_TRADABLE(HttpStatus.BAD_REQUEST, "거래할 수 없는 아이템입니다."),
    INVENTORY_INSUFFICIENT(HttpStatus.BAD_REQUEST, "사용 가능한 아이템 수량이 부족합니다."),
    HOME_NOT_FOUND(HttpStatus.NOT_FOUND, "마이 홈을 찾을 수 없습니다."),
    HOME_PRIVATE(HttpStatus.FORBIDDEN, "비공개 마이 홈입니다."),
    HOME_FURNITURE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "가구는 최대 20개까지 배치할 수 있습니다."),
    HOME_ALREADY_LIKED_TODAY(HttpStatus.CONFLICT, "오늘 이미 좋아요를 눌렀습니다."),
    HOME_SELF_LIKE(HttpStatus.BAD_REQUEST, "내 마이 홈에는 좋아요를 누를 수 없습니다."),

    // ===== Wallet / Reward =====
    WALLET_INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "코인이 부족합니다."),
    REWARD_ALREADY_CLAIMED(HttpStatus.CONFLICT, "오늘은 이미 참여했습니다."),
    REWARD_DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "오늘 참여 횟수를 모두 사용했습니다."),

    // ===== Shop =====
    SHOP_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    SHOP_PRODUCT_NOT_ON_SALE(HttpStatus.CONFLICT, "지금은 판매하지 않는 상품입니다."),

    // ===== Auction =====
    AUCTION_LISTING_NOT_FOUND(HttpStatus.NOT_FOUND, "매물을 찾을 수 없습니다."),
    AUCTION_LISTING_NOT_OWNED(HttpStatus.FORBIDDEN, "내 매물이 아닙니다."),
    AUCTION_LISTING_NOT_ON_SALE(HttpStatus.CONFLICT, "이미 종료된 매물입니다."),
    AUCTION_SELF_PURCHASE(HttpStatus.BAD_REQUEST, "내 매물은 구매할 수 없습니다."),
    AUCTION_LISTING_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "동시에 등록할 수 있는 매물은 10건까지입니다."),

    // ===== Community =====
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공지를 찾을 수 없습니다."),

    // ===== Notification / Companion (M3 / v2) =====
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다."),
    COMPANION_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 전용 토큰입니다."),
    ;

    private final HttpStatus status;
    private final String description;
}
