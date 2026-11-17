package com.gorapaduk.makingpet.wallet.controller;

import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.common.dto.PageResponse;
import com.gorapaduk.makingpet.wallet.domain.TransactionReason;
import com.gorapaduk.makingpet.wallet.dto.TransactionResponse;
import com.gorapaduk.makingpet.wallet.dto.WalletResponse;
import com.gorapaduk.makingpet.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 기획/04_api/07_명세서_Wallet_Reward.md W-1, W-2 */
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ApiResponse<WalletResponse> balance(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(walletService.getBalance(UUID.fromString(jwt.getSubject())), SuccessCode.READ);
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<TransactionResponse>> transactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TransactionReason reason,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(walletService.listTransactions(UUID.fromString(jwt.getSubject()), reason, page, size),
                SuccessCode.READ);
    }
}
