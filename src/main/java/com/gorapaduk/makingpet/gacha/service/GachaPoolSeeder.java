package com.gorapaduk.makingpet.gacha.service;

import java.util.Map;
import java.util.UUID;

import com.gorapaduk.makingpet.gacha.domain.GachaPool;
import com.gorapaduk.makingpet.pet.service.PetSpeciesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 상시 뽑기 풀 시드. 종 4마리 기준 가중치(만분율):
 * duck 3000 / dog 3000 (COMMON 60%) · cat 2800 (RARE 28%) · dinosaur 1200 (EPIC 12%).
 * 규칙 §1-1 의 LEGENDARY 2% 는 LEGENDARY 종이 생기면 dinosaur 에서 떼어 배분한다.
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class GachaPoolSeeder implements ApplicationRunner {

    public static final String STANDARD_POOL_NAME = "상시 뽑기";

    private final GachaService gachaService;
    private final PetSpeciesService speciesService;

    @Override
    public void run(ApplicationArguments args) {
        if (gachaService.existsPoolNamed(STANDARD_POOL_NAME)) return;
        Map<UUID, Integer> weights = Map.of(
                speciesService.getByCode("duck").getId(), 3000,
                speciesService.getByCode("dog").getId(), 3000,
                speciesService.getByCode("cat").getId(), 2800,
                speciesService.getByCode("dinosaur").getId(), 1200);
        gachaService.createPool(STANDARD_POOL_NAME, GachaPool.Type.STANDARD, 100, 900, 50, null, null, weights);
        log.info("상시 뽑기 풀 시드 생성");
    }
}
