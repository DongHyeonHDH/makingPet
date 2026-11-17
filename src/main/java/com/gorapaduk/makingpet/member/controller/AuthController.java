package com.gorapaduk.makingpet.member.controller;

import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.member.dto.LoginRequest;
import com.gorapaduk.makingpet.member.dto.MemberResponse;
import com.gorapaduk.makingpet.member.dto.RegisterRequest;
import com.gorapaduk.makingpet.member.dto.TokenResponse;
import com.gorapaduk.makingpet.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 기획/04_api/03_명세서_Member.md M-1, M-2 (M-3 refresh, M-4 logout 은 M2). */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.created(memberService.register(request), SuccessCode.MEMBER_CREATED);
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(memberService.login(request), SuccessCode.MEMBER_LOGIN);
    }
}
