package com.gorapaduk.makingpet.admin.dto;

import java.util.List;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.Rarity;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.pet.domain.PetSpecies;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 기획/04_api/10_명세서_Community_Admin.md AD-1 ~ AD-5.
 * 변경 불가 필드(code · rarity · category)는 PATCH 요청에도 받아 두고, 값이 오면 서비스 전에 400 으로 막는다
 * (조용히 무시하면 운영자가 바뀐 줄 안다).
 */
public final class AdminCatalogDtos {

    private AdminCatalogDtos() {}

    public static final String CODE_PATTERN = "^[a-z][a-z0-9_]{1,39}$";

    // ----- 펫 종 -----

    /** AD-1 */
    public record SpeciesCreateRequest(
            @NotBlank @Pattern(regexp = CODE_PATTERN) String code,
            @NotBlank @Size(max = 40) String name,
            @NotNull Rarity rarity,
            @NotBlank @Size(max = 200) String baseAssetKey,
            @Size(max = 300) String description,
            Boolean active) {}

    /** AD-2 */
    public record SpeciesUpdateRequest(
            @Size(min = 1, max = 40) String name,
            @Size(min = 1, max = 200) String baseAssetKey,
            @Size(max = 300) String description,
            Boolean active,
            String code,
            Rarity rarity) {}

    /** AD-1 · AD-2 응답: 종 전체 */
    public record SpeciesResponse(UUID id, String code, String name, Rarity rarity, String baseAssetKey,
                                  String description, boolean active) {
        public static SpeciesResponse from(PetSpecies s) {
            return new SpeciesResponse(s.getId(), s.getCode(), s.getName(), s.getRarity(), s.getBaseAssetKey(),
                    s.getDescription(), s.isActive());
        }
    }

    /** AD-3 */
    public record MotionsRequest(@NotNull @Size(max = 4) List<@Valid @NotNull MotionMapping> motions) {}

    public record MotionMapping(@NotNull @Min(2) @Max(5) Integer affectionLevel, @NotNull UUID motionItemId) {}

    public record MotionsResponse(UUID speciesId, List<MotionMapping> motions) {}

    // ----- 아이템 -----

    /** AD-4 */
    public record ItemCreateRequest(
            @NotBlank @Pattern(regexp = CODE_PATTERN) String code,
            @NotBlank @Size(max = 40) String name,
            @NotNull ItemCategory category,
            @NotNull Rarity rarity,
            @NotBlank @Size(max = 200) String assetKey,
            @Size(max = 300) String description,
            Boolean tradable,
            Boolean active) {}

    /** AD-5 */
    public record ItemUpdateRequest(
            @Size(min = 1, max = 40) String name,
            @Size(min = 1, max = 200) String assetKey,
            @Size(max = 300) String description,
            Boolean tradable,
            Boolean active,
            String code,
            ItemCategory category,
            Rarity rarity) {}
}
