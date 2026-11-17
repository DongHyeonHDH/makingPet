package com.gorapaduk.makingpet.admin.controller;

import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.community.dto.NoticeDtos;
import com.gorapaduk.makingpet.community.service.NoticeService;
import com.gorapaduk.makingpet.shop.dto.ShopDtos;
import com.gorapaduk.makingpet.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 기획/04_api/10_명세서_Community_Admin.md AD-8 ~ AD-12 (🛡) */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminShopNoticeController {

    private final ShopService shopService;
    private final NoticeService noticeService;

    /** AD-8 */
    @PostMapping("/shop/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShopDtos.AdminProduct> createProduct(@Valid @RequestBody ShopDtos.CreateRequest req) {
        return ApiResponse.created(shopService.create(req), SuccessCode.ADMIN_CREATED);
    }

    /** AD-9 */
    @PatchMapping("/shop/products/{productId}")
    public ApiResponse<ShopDtos.AdminProduct> updateProduct(@PathVariable UUID productId,
                                                           @Valid @RequestBody ShopDtos.UpdateRequest req) {
        return ApiResponse.ok(shopService.update(productId, req), SuccessCode.ADMIN_UPDATED);
    }

    /** AD-10. 작성자 = 요청한 관리자. */
    @PostMapping("/notices")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoticeDtos.NoticeDetail> createNotice(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody NoticeDtos.CreateRequest req) {
        return ApiResponse.created(noticeService.create(UUID.fromString(jwt.getSubject()), req), SuccessCode.NOTICE_CREATED);
    }

    /** AD-11 */
    @PatchMapping("/notices/{noticeId}")
    public ApiResponse<NoticeDtos.NoticeDetail> updateNotice(@PathVariable UUID noticeId,
                                                            @Valid @RequestBody NoticeDtos.UpdateRequest req) {
        return ApiResponse.ok(noticeService.update(noticeId, req), SuccessCode.NOTICE_UPDATED);
    }

    /** AD-12 soft delete. 200, data 없음. */
    @DeleteMapping("/notices/{noticeId}")
    public ApiResponse<Void> deleteNotice(@PathVariable UUID noticeId) {
        noticeService.delete(noticeId);
        return ApiResponse.ok(SuccessCode.NOTICE_DELETED);
    }
}
