package com.gorapaduk.makingpet.shop.controller;

import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.common.dto.PageResponse;
import com.gorapaduk.makingpet.common.idempotency.IdempotencyService;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.shop.dto.ShopDtos;
import com.gorapaduk.makingpet.shop.service.ShopService;
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

/** 기획/04_api/08_명세서_Shop.md S-1 ~ S-3 */
@RestController
@RequestMapping("/api/v1/shop/products")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final IdempotencyService idempotencyService;

    /** S-1 🔓 — 익명이면 jwt 가 null */
    @GetMapping
    public ApiResponse<PageResponse<ShopDtos.Product>> list(@AuthenticationPrincipal Jwt jwt,
                                                           @RequestParam(required = false) ItemCategory category,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(shopService.list(category, page, size, viewer(jwt)), SuccessCode.READ);
    }

    /** S-2 🔓 */
    @GetMapping("/{productId}")
    public ApiResponse<ShopDtos.Product> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
        return ApiResponse.ok(shopService.detail(productId, viewer(jwt)), SuccessCode.READ);
    }

    /** S-3 ✅ 🔑 */
    @PostMapping("/{productId}/purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShopDtos.PurchaseResponse> purchase(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable UUID productId,
                                                           @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                           @Valid @RequestBody(required = false) ShopDtos.PurchaseRequest request) {
        UUID memberId = UUID.fromString(jwt.getSubject());
        int quantity = request == null ? 1 : request.quantityOrDefault();
        var result = idempotencyService.execute(memberId, idempotencyKey, new PurchaseFingerprint(productId, quantity),
                ShopDtos.PurchaseResponse.class, () -> shopService.purchase(memberId, productId, quantity));
        return ApiResponse.created(result, result.item().name() + " " + result.quantity() + "개를 구매했습니다.");
    }

    /** 멱등 키 해시 대상. Map.of 는 순회 순서가 JVM 마다 달라 재기동 뒤 같은 요청이 다른 해시가 될 수 있어 record 로 고정한다. */
    record PurchaseFingerprint(UUID productId, int quantity) {}

    private static UUID viewer(Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }
}
