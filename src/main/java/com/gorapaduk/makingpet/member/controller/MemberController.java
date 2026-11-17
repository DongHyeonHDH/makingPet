package com.gorapaduk.makingpet.member.controller;

import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.member.dto.MemberResponse;
import com.gorapaduk.makingpet.member.dto.NicknameUpdateRequest;
import com.gorapaduk.makingpet.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 기획/04_api/03_명세서_Member.md M-5, M-6 (M-7 탈퇴는 M3). */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(memberService.getMe(memberId(jwt)), SuccessCode.READ);
    }

    @PatchMapping("/me")
    public ApiResponse<MemberResponse> updateMe(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody NicknameUpdateRequest request) {
        return ApiResponse.ok(memberService.changeNickname(memberId(jwt), request), SuccessCode.MEMBER_UPDATED);
    }

    /** JwtDecoder 검증기가 sub 가 UUID 임을 이미 보장한다. */
    static UUID memberId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
