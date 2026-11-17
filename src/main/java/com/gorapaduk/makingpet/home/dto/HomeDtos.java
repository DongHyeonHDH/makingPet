package com.gorapaduk.makingpet.home.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gorapaduk.makingpet.home.domain.Home;
import com.gorapaduk.makingpet.item.dto.ItemSummary;
import com.gorapaduk.makingpet.pet.dto.PetSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class HomeDtos {

    private HomeDtos() {}

    /** 홈 주인 정보. member 도메인이 컨트롤러에서 넘겨준다 (HomeService 는 MemberService 를 import 하지 않는다). */
    public record Owner(UUID id, String nickname, UUID representativePetId) {}

    public record OwnerPublic(UUID id, String nickname) {}

    /** H-1 / H-5 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HomeResponse(OwnerPublic owner, Home.Visibility visibility, int likeCount,
                               PetSummary representativePet, ItemSummary theme, List<Furniture> furniture,
                               Integer furnitureLimit) {}

    public record Furniture(UUID id, ItemSummary item, int x, int y, int layer) {}

    /** H-2 */
    public record ThemeRequest(@NotNull UUID themeItemId) {}

    /** H-3 */
    public record FurnitureRequest(@NotNull @Size(max = Home.FURNITURE_LIMIT) List<@Valid Placement> furniture) {}

    public record Placement(@NotNull UUID itemId, @Min(0) @Max(100) int x, @Min(0) @Max(100) int y, @Min(0) @Max(99) int layer) {}

    /** H-4 */
    public record VisibilityRequest(@NotNull Home.Visibility visibility) {}
}
