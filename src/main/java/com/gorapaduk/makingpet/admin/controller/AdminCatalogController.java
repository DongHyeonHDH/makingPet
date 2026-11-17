package com.gorapaduk.makingpet.admin.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.gorapaduk.makingpet.admin.dto.AdminCatalogDtos;
import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.dto.ItemSummary;
import com.gorapaduk.makingpet.item.service.ItemService;
import com.gorapaduk.makingpet.pet.domain.PetSpecies;
import com.gorapaduk.makingpet.pet.service.PetSpeciesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기획/04_api/10_명세서_Community_Admin.md AD-1 ~ AD-5 (🛡 ADMIN 이상, SecurityConfig 의 /api/v1/admin/**).
 * 규칙은 각 도메인 서비스(PetSpeciesService · ItemService)에 있고, 여기서는 요청 검증과 변환만 한다.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final PetSpeciesService speciesService;
    private final ItemService itemService;

    /** AD-1 */
    @PostMapping("/pet-species")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminCatalogDtos.SpeciesResponse> createSpecies(@Valid @RequestBody AdminCatalogDtos.SpeciesCreateRequest req) {
        PetSpecies s = speciesService.create(req.code(), req.name().strip(), req.rarity(), req.baseAssetKey(), req.description());
        if (Boolean.FALSE.equals(req.active())) s = speciesService.update(s.getId(), null, null, null, false);
        return ApiResponse.created(AdminCatalogDtos.SpeciesResponse.from(s), SuccessCode.ADMIN_CREATED);
    }

    /** AD-2. code · rarity 변경 불가. */
    @PatchMapping("/pet-species/{speciesId}")
    public ApiResponse<AdminCatalogDtos.SpeciesResponse> updateSpecies(@PathVariable UUID speciesId,
                                                                      @Valid @RequestBody AdminCatalogDtos.SpeciesUpdateRequest req) {
        rejectImmutable(req.code() != null, "code");
        rejectImmutable(req.rarity() != null, "rarity");
        PetSpecies s = speciesService.update(speciesId, req.name(), req.baseAssetKey(), req.description(), req.active());
        return ApiResponse.ok(AdminCatalogDtos.SpeciesResponse.from(s), SuccessCode.ADMIN_UPDATED);
    }

    /** AD-3. 전체 교체. */
    @PutMapping("/pet-species/{speciesId}/motions")
    public ApiResponse<AdminCatalogDtos.MotionsResponse> replaceMotions(@PathVariable UUID speciesId,
                                                                       @Valid @RequestBody AdminCatalogDtos.MotionsRequest req) {
        Map<Integer, UUID> mapping = new LinkedHashMap<>();
        for (var m : req.motions()) {
            if (mapping.put(m.affectionLevel(), m.motionItemId()) != null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "affectionLevel 이 중복되었습니다: " + m.affectionLevel());
            }
        }
        speciesService.replaceMotions(speciesId, mapping);
        var motions = speciesService.motionsOf(speciesId).stream()
                .map(sm -> new AdminCatalogDtos.MotionMapping(sm.getAffectionLevel(), sm.getMotionItemId()))
                .toList();
        return ApiResponse.ok(new AdminCatalogDtos.MotionsResponse(speciesId, motions), SuccessCode.ADMIN_UPDATED);
    }

    /** AD-4. MOTION 은 tradable 이 강제로 false (Item.create). */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ItemSummary> createItem(@Valid @RequestBody AdminCatalogDtos.ItemCreateRequest req) {
        Item item = itemService.create(req.code(), req.name().strip(), req.category(), req.rarity(), req.assetKey(),
                req.description(), req.tradable() == null || req.tradable());
        if (Boolean.FALSE.equals(req.active())) item = itemService.update(item.getId(), null, null, null, null, false);
        return ApiResponse.created(ItemSummary.detail(item), SuccessCode.ADMIN_CREATED);
    }

    /** AD-5. code · category · rarity 변경 불가. */
    @PatchMapping("/items/{itemId}")
    public ApiResponse<ItemSummary> updateItem(@PathVariable UUID itemId, @Valid @RequestBody AdminCatalogDtos.ItemUpdateRequest req) {
        rejectImmutable(req.code() != null, "code");
        rejectImmutable(req.category() != null, "category");
        rejectImmutable(req.rarity() != null, "rarity");
        Item item = itemService.update(itemId, req.name(), req.assetKey(), req.description(), req.tradable(), req.active());
        return ApiResponse.ok(ItemSummary.detail(item), SuccessCode.ADMIN_UPDATED);
    }

    private static void rejectImmutable(boolean sent, String field) {
        if (sent) throw new BusinessException(ErrorCode.VALIDATION_FAILED, field + " 은(는) 변경할 수 없습니다. 새로 등록하세요.");
    }
}
