package com.gorapaduk.makingpet.pet.service;

import java.util.List;
import java.util.Map;

import com.gorapaduk.makingpet.common.constants.Rarity;
import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.item.service.ItemService;
import com.gorapaduk.makingpet.pet.domain.PetSpecies;
import com.gorapaduk.makingpet.pet.repository.PetSpeciesRepository;
import com.gorapaduk.makingpet.pet.repository.SpeciesMotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 펫 종 4종(docs/pet-character-portraits) + 종별 2단계 해금 움직임 시드.
 * ItemCatalogSeeder 뒤에 실행된다 (@Order). Flyway 도입(M3) 시 SQL 로 옮긴다.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class PetSpeciesSeeder implements ApplicationRunner {

    private final PetSpeciesRepository species;
    private final SpeciesMotionRepository motions;
    private final ItemService itemService;
    private final PetSpeciesService speciesService;

    private record Seed(String code, String name, Rarity rarity, String description, String motionCode, String motionName) {}

    private static final List<Seed> SEEDS = List.of(
            new Seed("duck", "오리", Rarity.COMMON, "멍하니 서 있는 게 특기. 가끔 꽥 한다.", "motion_duck_waddle", "뒤뚱뒤뚱"),
            new Seed("dog", "강아지", Rarity.COMMON, "꼬리가 먼저 반응하고 머리는 나중에 따라온다.", "motion_dog_wag", "꼬리 흔들기"),
            new Seed("cat", "고양이", Rarity.RARE, "관심 없는 척하지만 다 보고 있다.", "motion_cat_stretch", "기지개"),
            new Seed("dinosaur", "공룡", Rarity.EPIC, "작지만 공룡이다. 본인은 무섭다고 생각한다.", "motion_dino_stomp", "쿵쿵 걷기")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        for (Seed seed : SEEDS) {
            if (species.existsByCode(seed.code())) continue;
            PetSpecies s = species.save(PetSpecies.create(seed.code(), seed.name(), seed.rarity(),
                    "pets/" + seed.code() + "/base.png", seed.description()));
            // getByCode() 는 없으면 @Transactional 경계에서 예외를 던져 이 트랜잭션을 rollback-only 로 만든다.
            // (catch 해도 커밋 시 UnexpectedRollbackException) → 예외 없는 Optional 조회를 쓴다.
            Item motion = itemService.findByCode(seed.motionCode())
                    .orElseGet(() -> itemService.create(seed.motionCode(), seed.motionName(), ItemCategory.MOTION, Rarity.COMMON,
                            "motion." + seed.code() + "." + seed.motionCode().substring(seed.motionCode().lastIndexOf('_') + 1),
                            seed.name() + " 전용 움직임 (친밀도 2단계)", false));
            speciesService.replaceMotions(s.getId(), Map.of(2, motion.getId()));
            created++;
        }
        if (created > 0) log.info("펫 종 시드 {}건 생성", created);
    }
}
