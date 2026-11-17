package com.gorapaduk.makingpet.pet.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.constants.Rarity;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.item.service.ItemService;
import com.gorapaduk.makingpet.pet.domain.PetSpecies;
import com.gorapaduk.makingpet.pet.domain.SpeciesMotion;
import com.gorapaduk.makingpet.pet.repository.PetSpeciesRepository;
import com.gorapaduk.makingpet.pet.repository.SpeciesMotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 펫 종 카탈로그 + 해금 움직임 매핑. AD-1~AD-3 은 admin 도메인이 이 서비스를 호출한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetSpeciesService {

    private final PetSpeciesRepository species;
    private final SpeciesMotionRepository motions;
    private final ItemService itemService;

    public PetSpecies getById(UUID id) {
        return species.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PET_SPECIES_NOT_FOUND));
    }

    public PetSpecies getByCode(String code) {
        return species.findByCode(code).orElseThrow(() -> new BusinessException(ErrorCode.PET_SPECIES_NOT_FOUND));
    }

    public List<PetSpecies> listActive() {
        return species.findByActiveTrueOrderByRarityAscNameAsc();
    }

    public Map<UUID, PetSpecies> getMap(Collection<UUID> ids) {
        return species.findByIdIn(ids).stream().collect(Collectors.toMap(PetSpecies::getId, Function.identity()));
    }

    /** 종의 해금 움직임 (단계 오름차순) */
    public List<SpeciesMotion> motionsOf(UUID speciesId) {
        return motions.findByIdSpeciesIdOrderByIdAffectionLevelAsc(speciesId);
    }

    /** AD-1 */
    @Transactional
    public PetSpecies create(String code, String name, Rarity rarity, String baseAssetKey, String description) {
        if (species.existsByCode(code)) throw new BusinessException(ErrorCode.PET_SPECIES_CODE_DUPLICATED);
        return species.save(PetSpecies.create(code, name, rarity, baseAssetKey, description));
    }

    /** AD-2 */
    @Transactional
    public PetSpecies update(UUID id, String name, String baseAssetKey, String description, Boolean active) {
        PetSpecies s = getById(id);
        s.update(name, baseAssetKey, description, active);
        return s;
    }

    /** AD-3 전체 교체. level 2~5, 아이템은 MOTION 카테고리여야 한다. */
    @Transactional
    public void replaceMotions(UUID speciesId, Map<Integer, UUID> levelToMotionItem) {
        getById(speciesId);
        for (var e : levelToMotionItem.entrySet()) {
            if (e.getKey() < 2 || e.getKey() > 5) throw new BusinessException(ErrorCode.VALIDATION_FAILED, "affectionLevel 은 2~5");
            if (itemService.getById(e.getValue()).getCategory() != ItemCategory.MOTION)
                throw new BusinessException(ErrorCode.ITEM_SLOT_MISMATCH);
        }
        motions.deleteByIdSpeciesId(speciesId);
        motions.flush();
        levelToMotionItem.forEach((level, item) -> motions.save(SpeciesMotion.of(speciesId, level, item)));
    }
}
