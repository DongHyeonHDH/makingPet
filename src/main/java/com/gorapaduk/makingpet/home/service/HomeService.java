package com.gorapaduk.makingpet.home.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.common.util.KstTime;
import com.gorapaduk.makingpet.home.domain.Home;
import com.gorapaduk.makingpet.home.domain.HomeFurniture;
import com.gorapaduk.makingpet.home.domain.HomeLike;
import com.gorapaduk.makingpet.home.dto.HomeDtos;
import com.gorapaduk.makingpet.home.repository.HomeFurnitureRepository;
import com.gorapaduk.makingpet.home.repository.HomeLikeRepository;
import com.gorapaduk.makingpet.home.repository.HomeRepository;
import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.item.dto.ItemSummary;
import com.gorapaduk.makingpet.item.service.InventoryService;
import com.gorapaduk.makingpet.item.service.ItemService;
import com.gorapaduk.makingpet.pet.dto.PetSummary;
import com.gorapaduk.makingpet.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 규칙 §3-2. 테마·가구는 인벤토리 reserve 로 잡는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final HomeRepository homes;
    private final HomeFurnitureRepository furnitures;
    private final HomeLikeRepository likes;
    private final ItemService itemService;
    private final InventoryService inventoryService;
    private final PetService petService;

    /** 가입 시 호출 (member 도메인). 기본 테마는 이미 인벤토리에 있어야 하며 여기서 reserve 한다. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void create(UUID memberId, UUID themeItemId) {
        if (homes.existsById(memberId)) return;
        inventoryService.reserve(memberId, themeItemId, 1);
        homes.save(Home.create(memberId, themeItemId));
    }

    /** H-1 */
    public HomeDtos.HomeResponse getMine(HomeDtos.Owner owner) {
        return render(getHome(owner.id()), owner, true);
    }

    /** H-5. 비공개면 본인만. */
    public HomeDtos.HomeResponse visit(HomeDtos.Owner owner, UUID viewerId) {
        Home home = homes.findById(owner.id()).orElseThrow(() -> new BusinessException(ErrorCode.HOME_NOT_FOUND));
        boolean isOwner = owner.id().equals(viewerId);
        if (!home.isPublic() && !isOwner) throw new BusinessException(ErrorCode.HOME_PRIVATE);
        return render(home, owner, isOwner);
    }

    /** H-2 */
    @Transactional
    public HomeDtos.HomeResponse changeTheme(HomeDtos.Owner owner, UUID themeItemId) {
        Home home = homes.findForUpdate(owner.id()).orElseThrow(() -> new BusinessException(ErrorCode.HOME_NOT_FOUND));
        if (itemService.getById(themeItemId).getCategory() != ItemCategory.THEME) throw new BusinessException(ErrorCode.ITEM_SLOT_MISMATCH);
        if (!home.getThemeItemId().equals(themeItemId)) {
            inventoryService.reserve(owner.id(), themeItemId, 1);           // 없으면 INVENTORY_INSUFFICIENT
            inventoryService.release(owner.id(), home.getThemeItemId(), 1);
            home.changeTheme(themeItemId);
        }
        return render(home, owner, true);
    }

    /** H-3 전체 교체: 기존 배치의 reserve 를 전부 풀고 새 배치만큼 다시 잡는다. */
    @Transactional
    public HomeDtos.HomeResponse replaceFurniture(HomeDtos.Owner owner, List<HomeDtos.Placement> placements) {
        Home home = homes.findForUpdate(owner.id()).orElseThrow(() -> new BusinessException(ErrorCode.HOME_NOT_FOUND));
        if (placements.size() > Home.FURNITURE_LIMIT) throw new BusinessException(ErrorCode.HOME_FURNITURE_LIMIT_EXCEEDED);

        Map<UUID, Integer> delta = new HashMap<>();
        furnitures.findByMemberIdOrderByLayerAsc(owner.id()).forEach(f -> delta.merge(f.getItemId(), -1, Integer::sum));
        Map<UUID, Item> items = itemService.getMap(placements.stream().map(HomeDtos.Placement::itemId).toList());
        for (HomeDtos.Placement p : placements) {
            Item item = items.get(p.itemId());
            if (item == null) throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
            if (item.getCategory() != ItemCategory.FURNITURE) throw new BusinessException(ErrorCode.ITEM_SLOT_MISMATCH);
            delta.merge(p.itemId(), 1, Integer::sum);
        }
        delta.forEach((itemId, d) -> { if (d < 0) inventoryService.release(owner.id(), itemId, -d); });
        delta.forEach((itemId, d) -> { if (d > 0) inventoryService.reserve(owner.id(), itemId, d); });

        furnitures.deleteByMemberId(owner.id());
        furnitures.flush();
        placements.forEach(p -> furnitures.save(HomeFurniture.of(owner.id(), p.itemId(), p.x(), p.y(), p.layer())));
        return render(home, owner, true);
    }

    /** H-4 */
    @Transactional
    public HomeDtos.HomeResponse changeVisibility(HomeDtos.Owner owner, Home.Visibility visibility) {
        Home home = homes.findForUpdate(owner.id()).orElseThrow(() -> new BusinessException(ErrorCode.HOME_NOT_FOUND));
        home.changeVisibility(visibility);
        return render(home, owner, true);
    }

    /** H-6 좋아요 (1일 1회, KST) */
    @Transactional
    public int like(UUID ownerId, UUID likerId) {
        if (ownerId.equals(likerId)) throw new BusinessException(ErrorCode.HOME_SELF_LIKE);
        Home home = homes.findForUpdate(ownerId).orElseThrow(() -> new BusinessException(ErrorCode.HOME_NOT_FOUND));
        if (!home.isPublic()) throw new BusinessException(ErrorCode.HOME_PRIVATE);
        LocalDate today = KstTime.today();
        // 이미 눌렀으면 INSERT 를 시도하지 않는다 (flush 실패로 영속성 컨텍스트가 오염되는 것을 피한다).
        // 아래 유니크 제약 catch 는 동시 요청 경합용 안전망으로만 남긴다.
        if (likes.existsByLikerIdAndOwnerIdAndLikeDate(likerId, ownerId, today)) {
            throw new BusinessException(ErrorCode.HOME_ALREADY_LIKED_TODAY);
        }
        try {
            likes.saveAndFlush(HomeLike.of(likerId, ownerId, today));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.HOME_ALREADY_LIKED_TODAY);
        }
        home.like();
        return home.getLikeCount();
    }

    // ----- 내부 -----

    private Home getHome(UUID memberId) {
        return homes.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.HOME_NOT_FOUND));
    }

    private HomeDtos.HomeResponse render(Home home, HomeDtos.Owner owner, boolean isOwner) {
        List<HomeFurniture> rows = furnitures.findByMemberIdOrderByLayerAsc(owner.id());
        Map<UUID, Item> items = itemService.getMap(rows.stream().map(HomeFurniture::getItemId).toList());
        List<HomeDtos.Furniture> furniture = rows.stream()
                .map(f -> new HomeDtos.Furniture(f.getId(), ItemSummary.from(items.get(f.getItemId())), f.getPosX(), f.getPosY(), f.getLayer()))
                .toList();
        PetSummary rep = owner.representativePetId() == null ? null
                : petService.summary(owner.representativePetId(), owner.representativePetId());
        return new HomeDtos.HomeResponse(new HomeDtos.OwnerPublic(owner.id(), owner.nickname()), home.getVisibility(),
                home.getLikeCount(), rep, ItemSummary.from(itemService.getById(home.getThemeItemId())), furniture,
                isOwner ? Home.FURNITURE_LIMIT : null);
    }
}
