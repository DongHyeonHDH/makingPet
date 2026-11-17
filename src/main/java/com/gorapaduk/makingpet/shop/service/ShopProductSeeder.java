package com.gorapaduk.makingpet.shop.service;

import java.util.List;

import com.gorapaduk.makingpet.item.service.ItemService;
import com.gorapaduk.makingpet.shop.domain.ShopProduct;
import com.gorapaduk.makingpet.shop.repository.ShopProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상점 초기 상품 시드 (규칙 §6: 딸기 리본 70 · 새싹 모자 90 · 구름 티셔츠 120 · 노란 장화 80 · 폭신한 러그 110).
 * 상시 판매(기간 없음). 같은 아이템 상품이 이미 있으면 건너뛴다. ItemCatalogSeeder 뒤에 실행된다.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class ShopProductSeeder implements ApplicationRunner {

    private final ShopProductRepository products;
    private final ItemService itemService;

    private record Seed(String itemCode, int price, int sortOrder) {}

    private static final List<Seed> SEEDS = List.of(
            new Seed("ribbon", 70, 10),
            new Seed("sprout_hat", 90, 20),
            new Seed("cloud_tee", 120, 30),
            new Seed("yellow_boots", 80, 40),
            new Seed("fluffy_rug", 110, 50)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        for (Seed seed : SEEDS) {
            // 예외 없는 Optional 조회: getByCode() 실패는 이 트랜잭션을 rollback-only 로 만든다 (작업기록 2026-11-17).
            var item = itemService.findByCode(seed.itemCode());
            if (item.isEmpty() || products.existsByItemId(item.get().getId())) continue;
            products.save(ShopProduct.create(item.get().getId(), seed.price(), null, null, true, seed.sortOrder()));
            created++;
        }
        if (created > 0) log.info("상점 상품 시드 {}건 생성", created);
    }
}
