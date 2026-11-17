package com.gorapaduk.makingpet.item.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.item.dto.InventoryItemResponse;
import com.gorapaduk.makingpet.item.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 기획/04_api/06_명세서_Item_Home.md I-2 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ApiResponse<Map<String, List<InventoryItemResponse>>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) ItemCategory category) {
        var items = inventoryService.list(UUID.fromString(jwt.getSubject()), category);
        return ApiResponse.ok(Map.of("items", items), SuccessCode.READ);
    }
}
