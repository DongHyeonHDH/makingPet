package com.gorapaduk.makingpet.pet.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.common.exception.RetryAfterException;
import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.dto.ItemSummary;
import com.gorapaduk.makingpet.item.service.InventoryService;
import com.gorapaduk.makingpet.item.service.ItemService;
import com.gorapaduk.makingpet.pet.domain.AffectionLevel;
import com.gorapaduk.makingpet.pet.domain.EquipSlot;
import com.gorapaduk.makingpet.pet.domain.InteractionType;
import com.gorapaduk.makingpet.pet.domain.PetSpecies;
import com.gorapaduk.makingpet.pet.domain.SpeciesMotion;
import com.gorapaduk.makingpet.pet.domain.UserPet;
import com.gorapaduk.makingpet.pet.dto.CollectionResponse;
import com.gorapaduk.makingpet.pet.dto.InteractionResponse;
import com.gorapaduk.makingpet.pet.dto.PetDetailResponse;
import com.gorapaduk.makingpet.pet.dto.PetSummary;
import com.gorapaduk.makingpet.pet.dto.SpeciesSummary;
import com.gorapaduk.makingpet.pet.repository.UserPetRepository;
import com.gorapaduk.makingpet.wallet.domain.TransactionReason;
import com.gorapaduk.makingpet.wallet.dto.WalletResponse;
import com.gorapaduk.makingpet.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보유 펫. 대표 펫 ID 는 member 도메인이 들고 있으므로(members.representative_pet_id)
 * 이 서비스는 MemberService 를 import 하지 않고 representativePetId 를 인자로 받는다 (순환 의존 방지).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final UserPetRepository pets;
    private final PetSpeciesService speciesService;
    private final ItemService itemService;
    private final InventoryService inventoryService;
    private final WalletService walletService;

    // ----- 다른 도메인 컨트랙트 -----

    public boolean ownsSpecies(UUID memberId, UUID speciesId) {
        return pets.existsByMemberIdAndSpeciesId(memberId, speciesId);
    }

    public List<UUID> ownedSpeciesIds(UUID memberId) {
        return pets.findOwnedSpeciesIds(memberId);
    }

    public long countPets(UUID memberId) {
        return pets.countByMemberIdAndDeletedAtIsNull(memberId);
    }

    /** 뽑기 등에서 새 펫 생성. 호출자 트랜잭션에 참여. */
    @Transactional(propagation = Propagation.REQUIRED)
    public UserPet create(UUID memberId, PetSpecies species, String obtainedFrom) {
        return pets.save(UserPet.create(memberId, species, obtainedFrom));
    }

    public PetSummary summary(UUID petId, UUID representativePetId) {
        UserPet pet = getOwnedOrThrow(petId, null);
        return toSummary(pet, representativePetId, LocalDateTime.now(), itemMapFor(List.of(pet)));
    }

    // ----- P-1 ~ P-8 -----

    /** P-1 */
    public List<PetSummary> list(UUID memberId, UUID representativePetId) {
        List<UserPet> owned = pets.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtAsc(memberId);
        LocalDateTime now = LocalDateTime.now();
        Map<UUID, Item> items = itemMapFor(owned);
        Map<UUID, PetSpecies> species = speciesService.getMap(owned.stream().map(UserPet::getSpeciesId).toList());
        return owned.stream()
                .map(p -> toSummary(p, species.get(p.getSpeciesId()), representativePetId, now, items))
                .sorted((a, b) -> {
                    if (a.isRepresentative() != b.isRepresentative()) return a.isRepresentative() ? -1 : 1;
                    return b.species().rarity().compareTo(a.species().rarity());
                })
                .toList();
    }

    /** P-2 */
    public PetDetailResponse detail(UUID memberId, UUID petId, UUID representativePetId) {
        UserPet pet = getOwnedOrThrow(petId, memberId);
        PetSpecies species = speciesService.getById(pet.getSpeciesId());
        LocalDateTime now = LocalDateTime.now();
        PetSummary s = toSummary(pet, species, representativePetId, now, itemMapFor(List.of(pet)));
        List<ItemSummary> unlocked = unlockedMotions(species.getId(), pet.getAffectionLevel());
        Map<InteractionType, LocalDateTime> cooldowns = new EnumMap<>(InteractionType.class);
        for (InteractionType t : InteractionType.values()) {
            cooldowns.put(t, pet.isOnCooldown(t, now) ? pet.nextAvailable(t) : null);
        }
        return new PetDetailResponse(s.id(), s.name(), s.species(), s.mood(), s.moodState(), s.affectionLevel(),
                s.isRepresentative(), s.equipment(), pet.getAffection(),
                AffectionLevel.nextThreshold(pet.getAffectionLevel()), pet.getStage(), cooldowns, unlocked,
                pet.getCreatedAt());
    }

    /** P-3 */
    @Transactional
    public PetSummary rename(UUID memberId, UUID petId, String name, UUID representativePetId) {
        UserPet pet = getOwnedOrThrow(petId, memberId);
        String trimmed = name.strip();
        if (trimmed.isEmpty() || trimmed.length() > 12) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        if (trimmed.toLowerCase(Locale.ROOT).contains("admin")) throw new BusinessException(ErrorCode.PET_NAME_FORBIDDEN_WORD);
        pet.rename(trimmed);
        return toSummary(pet, representativePetId, LocalDateTime.now(), itemMapFor(List.of(pet)));
    }

    /** P-4 에서 소유 검증용 */
    public UserPet getOwnedOrThrow(UUID petId, UUID memberId) {
        UserPet pet = pets.findByIdAndDeletedAtIsNull(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
        if (memberId != null && !pet.getMemberId().equals(memberId)) throw new BusinessException(ErrorCode.PET_NOT_OWNED);
        return pet;
    }

    /**
     * P-5 상호작용. 펫 행을 잠근 뒤 쿨다운 검사 → 기분·친밀도 → 단계 상승 시 움직임 지급·코인. 한 트랜잭션.
     */
    @Transactional
    public InteractionResponse interact(UUID memberId, UUID petId, InteractionType type, UUID representativePetId) {
        UserPet pet = pets.findForUpdate(petId).orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
        if (!pet.getMemberId().equals(memberId)) throw new BusinessException(ErrorCode.PET_NOT_OWNED);
        LocalDateTime now = LocalDateTime.now();
        if (pet.isOnCooldown(type, now)) {
            long seconds = java.time.Duration.between(now, pet.nextAvailable(type)).toSeconds() + 1;
            throw new RetryAfterException(ErrorCode.PET_INTERACTION_COOLDOWN, seconds,
                    "아직 쉬는 중이에요. " + seconds + "초 뒤에 다시 해보세요.");
        }

        int before = pet.getAffectionLevel();
        int newLevel = pet.interact(type, now);
        PetSpecies species = speciesService.getById(pet.getSpeciesId());

        InteractionResponse.LevelUp levelUp = null;
        WalletResponse wallet = walletService.getBalance(memberId);
        if (newLevel > 0) {
            List<ItemSummary> granted = new ArrayList<>();
            for (SpeciesMotion m : speciesService.motionsOf(species.getId())) {
                if (m.getAffectionLevel() > before && m.getAffectionLevel() <= newLevel) {
                    inventoryService.grant(memberId, m.getMotionItemId(), 1);
                    granted.add(ItemSummary.from(itemService.getById(m.getMotionItemId())));
                }
            }
            int coins = 0;
            for (int lv = before + 1; lv <= newLevel; lv++) coins += AffectionLevel.coinReward(lv);
            if (coins > 0) {
                wallet = walletService.credit(memberId, coins, TransactionReason.AFFECTION_REWARD, "PET", petId);
            }
            levelUp = new InteractionResponse.LevelUp(before, newLevel, new InteractionResponse.Rewards(granted, coins));
        }

        String motionKey = type.getDefaultMotionKey();
        UUID equippedMotion = pet.getEquipment().get(EquipSlot.MOTION);
        if (equippedMotion != null) motionKey = itemService.getById(equippedMotion).getAssetKey();

        PetSummary summary = toSummary(pet, species, representativePetId, now, itemMapFor(List.of(pet)));
        return new InteractionResponse(summary, motionKey, type.getMoodDelta(), type.getAffectionDelta(), levelUp,
                pet.nextAvailable(type), wallet);
    }

    /** P-6 장착 전체 교체 */
    @Transactional
    public PetSummary replaceEquipment(UUID memberId, UUID petId, Map<EquipSlot, UUID> requested, UUID representativePetId) {
        UserPet pet = pets.findForUpdate(petId).orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
        if (!pet.getMemberId().equals(memberId)) throw new BusinessException(ErrorCode.PET_NOT_OWNED);

        Map<EquipSlot, UUID> next = new EnumMap<>(EquipSlot.class);
        if (requested != null) requested.forEach((slot, itemId) -> { if (itemId != null) next.put(slot, itemId); });

        // 1. 카테고리·해금 검증
        Map<UUID, Item> items = itemService.getMap(next.values());
        Set<UUID> unlocked = unlockedMotionIds(pet.getSpeciesId(), pet.getAffectionLevel());
        for (var e : next.entrySet()) {
            Item item = items.get(e.getValue());
            if (item == null) throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
            if (item.getCategory() != e.getKey().getCategory()) throw new BusinessException(ErrorCode.ITEM_SLOT_MISMATCH);
            if (e.getKey() == EquipSlot.MOTION && !unlocked.contains(item.getId()))
                throw new BusinessException(ErrorCode.PET_MOTION_NOT_UNLOCKED);
        }

        // 2. 예약 차이 계산: 이전 장착 release, 새 장착 reserve (같은 아이템은 상쇄)
        Map<UUID, Integer> delta = new HashMap<>();
        pet.getEquipment().forEach((slot, itemId) -> { if (slot.isReserves()) delta.merge(itemId, -1, Integer::sum); });
        next.forEach((slot, itemId) -> { if (slot.isReserves()) delta.merge(itemId, 1, Integer::sum); });
        delta.forEach((itemId, d) -> {
            if (d < 0) inventoryService.release(memberId, itemId, -d);
        });
        delta.forEach((itemId, d) -> {
            if (d > 0) inventoryService.reserve(memberId, itemId, d);      // 부족하면 INVENTORY_INSUFFICIENT
        });

        pet.replaceEquipment(next);
        return toSummary(pet, representativePetId, LocalDateTime.now(), itemMapFor(List.of(pet)));
    }

    /** P-7 도감 */
    public CollectionResponse collection(UUID memberId) {
        List<UserPet> owned = pets.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtAsc(memberId);
        Map<UUID, UserPet> bySpecies = owned.stream().collect(Collectors.toMap(UserPet::getSpeciesId, p -> p));
        List<CollectionResponse.Entry> entries = new ArrayList<>();
        for (PetSpecies s : speciesService.listActive()) {
            UserPet p = bySpecies.get(s.getId());
            entries.add(p == null
                    ? new CollectionResponse.Entry(SpeciesSummary.silhouette(s), false, null, null)
                    : new CollectionResponse.Entry(SpeciesSummary.detail(s), true, p.getId(), p.getCreatedAt()));
        }
        return new CollectionResponse(entries.size(), bySpecies.size(), entries);
    }

    /** P-8 놓아주기. 대표 펫·마지막 1마리는 불가. 장착 해제 후 소프트 딜리트. */
    @Transactional
    public void release(UUID memberId, UUID petId, UUID representativePetId) {
        UserPet pet = pets.findForUpdate(petId).orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
        if (!pet.getMemberId().equals(memberId)) throw new BusinessException(ErrorCode.PET_NOT_OWNED);
        if (petId.equals(representativePetId) || countPets(memberId) <= 1)
            throw new BusinessException(ErrorCode.PET_RELEASE_NOT_ALLOWED);
        pet.getEquipment().forEach((slot, itemId) -> { if (slot.isReserves()) inventoryService.release(memberId, itemId, 1); });
        pet.release();
    }

    // ----- 내부 -----

    private Set<UUID> unlockedMotionIds(UUID speciesId, int level) {
        Set<UUID> ids = new HashSet<>();
        ids.add(itemService.getByCode(Item.DEFAULT_MOTION_CODE).getId());
        for (SpeciesMotion m : speciesService.motionsOf(speciesId)) {
            if (m.getAffectionLevel() <= level) ids.add(m.getMotionItemId());
        }
        return ids;
    }

    private List<ItemSummary> unlockedMotions(UUID speciesId, int level) {
        return itemService.getMap(unlockedMotionIds(speciesId, level)).values().stream()
                .map(ItemSummary::from).toList();
    }

    private Map<UUID, Item> itemMapFor(List<UserPet> list) {
        Set<UUID> ids = new HashSet<>();
        list.forEach(p -> ids.addAll(p.getEquipment().values()));
        return ids.isEmpty() ? Map.of() : itemService.getMap(ids);
    }

    private PetSummary toSummary(UserPet pet, UUID representativePetId, LocalDateTime now, Map<UUID, Item> items) {
        return toSummary(pet, speciesService.getById(pet.getSpeciesId()), representativePetId, now, items);
    }

    private PetSummary toSummary(UserPet pet, PetSpecies species, UUID representativePetId, LocalDateTime now,
                                 Map<UUID, Item> items) {
        Map<EquipSlot, PetSummary.EquippedItem> equipment = new EnumMap<>(EquipSlot.class);
        for (EquipSlot slot : EquipSlot.values()) {
            UUID itemId = pet.getEquipment().get(slot);
            Item item = itemId == null ? null : items.get(itemId);
            equipment.put(slot, item == null ? null : new PetSummary.EquippedItem(item.getId(), item.getCode(), item.getAssetKey()));
        }
        if (equipment.get(EquipSlot.MOTION) == null) {
            Item idle = itemService.getByCode(Item.DEFAULT_MOTION_CODE);
            equipment.put(EquipSlot.MOTION, new PetSummary.EquippedItem(idle.getId(), idle.getCode(), idle.getAssetKey()));
        }
        int mood = pet.currentMood(now);
        return new PetSummary(pet.getId(), pet.getName(), SpeciesSummary.from(species), mood, PetSummary.MoodState.of(mood),
                pet.getAffectionLevel(), pet.getId().equals(representativePetId), equipment);
    }
}
