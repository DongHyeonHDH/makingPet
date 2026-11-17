package com.gorapaduk.makingpet.home.controller;

import java.util.Map;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.home.dto.HomeDtos;
import com.gorapaduk.makingpet.home.service.HomeService;
import com.gorapaduk.makingpet.member.domain.Member;
import com.gorapaduk.makingpet.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 기획/04_api/06_명세서_Item_Home.md H-1 ~ H-6 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final MemberService memberService;

    @GetMapping("/home")
    public ApiResponse<HomeDtos.HomeResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(homeService.getMine(owner(jwt)), SuccessCode.READ);
    }

    @PutMapping("/home/theme")
    public ApiResponse<HomeDtos.HomeResponse> theme(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody HomeDtos.ThemeRequest req) {
        return ApiResponse.ok(homeService.changeTheme(owner(jwt), req.themeItemId()), SuccessCode.HOME_UPDATED);
    }

    @PutMapping("/home/furniture")
    public ApiResponse<HomeDtos.HomeResponse> furniture(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody HomeDtos.FurnitureRequest req) {
        return ApiResponse.ok(homeService.replaceFurniture(owner(jwt), req.furniture()), SuccessCode.HOME_UPDATED);
    }

    @PatchMapping("/home/visibility")
    public ApiResponse<HomeDtos.HomeResponse> visibility(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody HomeDtos.VisibilityRequest req) {
        return ApiResponse.ok(homeService.changeVisibility(owner(jwt), req.visibility()), SuccessCode.HOME_UPDATED);
    }

    /** H-5 🔓 — 익명이면 jwt 가 null */
    @GetMapping("/homes/{nickname}")
    public ApiResponse<HomeDtos.HomeResponse> visit(@AuthenticationPrincipal Jwt jwt, @PathVariable String nickname) {
        Member target = memberService.getByNickname(nickname);
        UUID viewer = jwt == null ? null : UUID.fromString(jwt.getSubject());
        return ApiResponse.ok(homeService.visit(toOwner(target), viewer), SuccessCode.READ);
    }

    /** H-6 (M3 COULD — 구현은 간단해 함께 넣음) */
    @PostMapping("/homes/{nickname}/likes")
    public ApiResponse<Map<String, Integer>> like(@AuthenticationPrincipal Jwt jwt, @PathVariable String nickname) {
        Member target = memberService.getByNickname(nickname);
        int count = homeService.like(target.getId(), UUID.fromString(jwt.getSubject()));
        return ApiResponse.ok(Map.of("likeCount", count), SuccessCode.OK);
    }

    private HomeDtos.Owner owner(Jwt jwt) {
        return toOwner(memberService.getActive(UUID.fromString(jwt.getSubject())));
    }

    private static HomeDtos.Owner toOwner(Member m) {
        return new HomeDtos.Owner(m.getId(), m.getNickname(), m.getRepresentativePetId());
    }
}
