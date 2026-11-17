package com.gorapaduk.makingpet.member.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** M-2 응답. refreshToken / refreshExpiresIn 은 M2 에서 채운다(그 전까지 null → 직렬화 생략). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        Long refreshExpiresIn) {

    public static TokenResponse accessOnly(String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, null, null);
    }
}
