package com.gorapaduk.makingpet.gacha.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.common.dto.PageResponse;
import com.gorapaduk.makingpet.common.idempotency.IdempotencyService;
import com.gorapaduk.makingpet.gacha.dto.GachaDtos;
import com.gorapaduk.makingpet.gacha.service.GachaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 기획/04_api/05_명세서_Gacha.md G-1 ~ G-4 */
@RestController
@RequestMapping("/api/v1/gacha")
@RequiredArgsConstructor
public class GachaController {

    private final GachaService gachaService;
    private final IdempotencyService idempotencyService;

    /** G-1 🔓 */
    @GetMapping("/pools")
    public ApiResponse<Map<String, List<GachaDtos.PoolResponse>>> pools() {
        return ApiResponse.ok(Map.of("items", gachaService.listOpenPools()), SuccessCode.READ);
    }

    /** G-2 🔑 */
    @PostMapping("/pools/{poolId}/draws")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GachaDtos.DrawResponse> draw(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable UUID poolId,
                                                    @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                    @Valid @RequestBody GachaDtos.DrawRequest request) {
        UUID memberId = UUID.fromString(jwt.getSubject());
        var result = idempotencyService.execute(memberId, idempotencyKey, Map.of("poolId", poolId, "count", request.count()),
                GachaDtos.DrawResponse.class, () -> gachaService.draw(memberId, poolId, request.count()));
        return ApiResponse.created(result, result.summary().newPets() > 0 ? SuccessCode.GACHA_NEW_PET : SuccessCode.GACHA_REFUND_ONLY);
    }

    /** G-3 */
    @GetMapping("/draws")
    public ApiResponse<PageResponse<GachaDtos.DrawRecord>> history(@AuthenticationPrincipal Jwt jwt,
                                                                   @RequestParam(required = false) UUID poolId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(gachaService.history(UUID.fromString(jwt.getSubject()), poolId, page, size), SuccessCode.READ);
    }

    /** G-4 */
    @GetMapping("/pools/{poolId}/pity")
    public ApiResponse<GachaDtos.PityResponse> pity(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID poolId) {
        return ApiResponse.ok(gachaService.pity(UUID.fromString(jwt.getSubject()), poolId), SuccessCode.READ);
    }
}
