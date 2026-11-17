package com.gorapaduk.makingpet.reward.controller;

import java.time.YearMonth;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.common.idempotency.IdempotencyService;
import com.gorapaduk.makingpet.reward.dto.RewardDtos;
import com.gorapaduk.makingpet.reward.service.RewardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 기획/04_api/07_명세서_Wallet_Reward.md R-1 ~ R-5 */
@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;
    private final IdempotencyService idempotencyService;

    @GetMapping("/today")
    public ApiResponse<RewardDtos.Today> today(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(rewardService.today(id(jwt)), SuccessCode.READ);
    }

    @PostMapping("/attendance")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RewardDtos.AttendanceResponse> attend(@AuthenticationPrincipal Jwt jwt) {
        var r = rewardService.attend(id(jwt));
        return ApiResponse.created(r, "출석 완료! " + (r.coins() + r.bonusCoins()) + "코인을 받았습니다.");
    }

    @GetMapping("/attendance")
    public ApiResponse<RewardDtos.AttendanceCalendar> calendar(@AuthenticationPrincipal Jwt jwt,
                                                               @RequestParam(required = false) String month) {
        YearMonth ym = month == null ? YearMonth.from(com.gorapaduk.makingpet.common.util.KstTime.today()) : YearMonth.parse(month);
        return ApiResponse.ok(rewardService.calendar(id(jwt), ym), SuccessCode.READ);
    }

    @PostMapping("/rps")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RewardDtos.RpsResponse> rps(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RewardDtos.RpsRequest req) {
        var r = rewardService.rps(id(jwt), req.hand());
        String msg = switch (r.result()) {
            case WIN -> "이겼습니다! " + r.coins() + "코인";
            case DRAW -> "비겼습니다. " + r.coins() + "코인";
            case LOSE -> "졌습니다… 그래도 " + r.coins() + "코인";
        };
        return ApiResponse.created(r, msg);
    }

    /** 🔑 */
    @PostMapping("/slot")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RewardDtos.SlotResponse> slot(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestHeader("Idempotency-Key") String key) {
        UUID memberId = id(jwt);
        var r = idempotencyService.execute(memberId, key, null, RewardDtos.SlotResponse.class, () -> rewardService.slot(memberId));
        String msg = r.payout() > 0 ? r.tier() + "! " + r.payout() + "코인" : "꽝… 다음 기회에";
        return ApiResponse.created(r, msg);
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
