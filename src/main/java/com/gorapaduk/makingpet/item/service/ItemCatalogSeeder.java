package com.gorapaduk.makingpet.item.service;

import java.util.List;

import com.gorapaduk.makingpet.common.constants.Rarity;
import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 초기 아이템 카탈로그 시드 (06_features/02_게임_시스템_규칙.md §6 + 기본 테마·기본 움직임).
 * code 가 이미 있으면 건너뛴다. Flyway 도입(M3) 시 R__seed.sql 로 옮긴다.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class ItemCatalogSeeder implements ApplicationRunner {

    private final ItemRepository items;

    private record Seed(String code, String name, ItemCategory category, Rarity rarity, String assetKey,
                        String description, boolean tradable) {}

    private static final List<Seed> SEEDS = List.of(
            // 시스템 기본 (거래 불가)
            new Seed(Item.DEFAULT_THEME_CODE, "기본 방", ItemCategory.THEME, Rarity.COMMON, "themes/theme_basic", "가입하면 받는 기본 테마", false),
            new Seed(Item.DEFAULT_MOTION_CODE, "가만히 있기", ItemCategory.MOTION, Rarity.COMMON, "motion.idle", "모든 펫의 기본 움직임", false),
            // 상점 초기 상품 (시안 web-preview/market.js)
            new Seed("ribbon", "딸기 리본", ItemCategory.ACCESSORY, Rarity.COMMON, "items/ribbon.png", "머리에 달면 두 배로 귀여워진다", true),
            new Seed("sprout_hat", "새싹 모자", ItemCategory.ACCESSORY, Rarity.COMMON, "items/sprout_hat.png", "물을 주면 자랄 것 같지만 자라지 않는다", true),
            new Seed("cloud_tee", "구름 티셔츠", ItemCategory.TOP, Rarity.RARE, "items/cloud_tee.png", "폭신폭신한 구름 무늬", true),
            new Seed("yellow_boots", "노란 장화", ItemCategory.ACCESSORY, Rarity.COMMON, "items/yellow_boots.png", "비 오는 날 신으면 신난다", true),
            new Seed("fluffy_rug", "폭신한 러그", ItemCategory.FURNITURE, Rarity.COMMON, "items/fluffy_rug.png", "펫이 제일 좋아하는 자리", true)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        for (Seed seed : SEEDS) {
            if (items.existsByCode(seed.code())) continue;
            items.save(Item.create(seed.code(), seed.name(), seed.category(), seed.rarity(), seed.assetKey(),
                    seed.description(), seed.tradable()));
            created++;
        }
        if (created > 0) log.info("아이템 카탈로그 시드 {}건 생성", created);
    }
}
