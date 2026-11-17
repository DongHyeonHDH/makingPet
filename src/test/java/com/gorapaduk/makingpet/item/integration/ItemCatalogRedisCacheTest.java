package com.gorapaduk.makingpet.item.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gorapaduk.makingpet.item.service.ItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 실제 Redis 로 I-1 카탈로그 캐시를 확인한다. CACHE_TYPE=redis 로 실행할 때만 돈다 (REDIS_HOST/REDIS_PORT 로 연결).
 * 커밋된 결과만 캐시되므로(transactionAware) 이 클래스는 @Transactional 을 붙이지 않는다.
 * Spring Data Redis 4 는 캐시 저장·비우기를 별도 연결에서 비동기로 처리하므로, 키 존재 여부는 잠깐 기다리며 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "CACHE_TYPE", matches = "redis")
class ItemCatalogRedisCacheTest {

    private static final String KEY = "makingpet:cache:itemCatalog::FURNITURE:0:7";

    @Autowired MockMvc mvc;
    @Autowired StringRedisTemplate redis;
    @Autowired ItemService itemService;

    private String fetch() throws Exception {
        return mvc.perform(get("/api/v1/items").param("category", "FURNITURE").param("size", "7"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("첫 조회 결과가 Redis 에 저장되고, 두 번째 조회는 같은 응답을 주며, 아이템 수정 시 비워진다")
    void cacheHitAndEvict() throws Exception {
        redis.delete(KEY);

        String first = fetch();
        awaitKey(true);
        assertThat(redis.opsForValue().get(KEY)).contains("fluffy_rug").doesNotContain("@class");
        assertThat(redis.getExpire(KEY)).isPositive();

        String second = fetch();                       // 캐시에서 역직렬화한 값
        assertThat(second).isEqualTo(first);

        var rug = itemService.getByCode("fluffy_rug");
        itemService.update(rug.getId(), null, null, null, null, null);   // 값은 그대로, 캐시 무효화만 발생
        awaitKey(false);
    }

    private void awaitKey(boolean expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3_000;
        while (redis.hasKey(KEY) != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertThat(redis.hasKey(KEY)).as("Redis key %s exists", KEY).isEqualTo(expected);
    }
}
