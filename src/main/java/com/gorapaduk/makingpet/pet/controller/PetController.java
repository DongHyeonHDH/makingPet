package com.gorapaduk.makingpet.pet.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.member.service.MemberService;
import com.gorapaduk.makingpet.pet.dto.CollectionResponse;
import com.gorapaduk.makingpet.pet.dto.InteractionResponse;
import com.gorapaduk.makingpet.pet.dto.PetDetailResponse;
import com.gorapaduk.makingpet.pet.dto.PetRequests;
import com.gorapaduk.makingpet.pet.dto.PetSummary;
import com.gorapaduk.makingpet.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 기획/04_api/04_명세서_Pet.md P-1 ~ P-8 */
@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final MemberService memberService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@AuthenticationPrincipal Jwt jwt) {
        UUID memberId = id(jwt);
        UUID rep = memberService.representativePetId(memberId);
        List<PetSummary> items = petService.list(memberId, rep);
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("items", items);
        data.put("representativePetId", rep);
        return ApiResponse.ok(data, SuccessCode.READ);
    }

    /** P-7 — `/{petId}` 보다 먼저 매칭되도록 고정 경로를 둔다. */
    @GetMapping("/collection")
    public ApiResponse<CollectionResponse> collection(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(petService.collection(id(jwt)), SuccessCode.READ);
    }

    @GetMapping("/{petId}")
    public ApiResponse<PetDetailResponse> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID petId) {
        UUID memberId = id(jwt);
        return ApiResponse.ok(petService.detail(memberId, petId, memberService.representativePetId(memberId)), SuccessCode.READ);
    }

    @PatchMapping("/{petId}")
    public ApiResponse<PetSummary> rename(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID petId,
                                          @Valid @RequestBody PetRequests.Rename request) {
        UUID memberId = id(jwt);
        return ApiResponse.ok(petService.rename(memberId, petId, request.name(), memberService.representativePetId(memberId)),
                SuccessCode.PET_UPDATED);
    }

    @PatchMapping("/{petId}/representative")
    public ApiResponse<PetSummary> representative(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID petId) {
        UUID memberId = id(jwt);
        petService.getOwnedOrThrow(petId, memberId);
        memberService.changeRepresentativePet(memberId, petId);
        return ApiResponse.ok(petService.summary(petId, petId), SuccessCode.PET_UPDATED);
    }

    @PostMapping("/{petId}/interactions")
    public ApiResponse<InteractionResponse> interact(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID petId,
                                                     @Valid @RequestBody PetRequests.Interact request) {
        UUID memberId = id(jwt);
        var result = petService.interact(memberId, petId, request.type(), memberService.representativePetId(memberId));
        return ApiResponse.ok(result, SuccessCode.PET_INTERACTED);
    }

    @PutMapping("/{petId}/equipment")
    public ApiResponse<PetSummary> equipment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID petId,
                                             @RequestBody Map<String, UUID> body) {
        UUID memberId = id(jwt);
        var slots = new java.util.EnumMap<com.gorapaduk.makingpet.pet.domain.EquipSlot, UUID>(
                com.gorapaduk.makingpet.pet.domain.EquipSlot.class);
        body.forEach((k, v) -> slots.put(com.gorapaduk.makingpet.pet.domain.EquipSlot.valueOf(k), v));
        return ApiResponse.ok(petService.replaceEquipment(memberId, petId, slots, memberService.representativePetId(memberId)),
                SuccessCode.PET_UPDATED);
    }

    @DeleteMapping("/{petId}")
    public ApiResponse<Void> release(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID petId) {
        UUID memberId = id(jwt);
        petService.release(memberId, petId, memberService.representativePetId(memberId));
        return ApiResponse.ok(SuccessCode.PET_RELEASED);
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
