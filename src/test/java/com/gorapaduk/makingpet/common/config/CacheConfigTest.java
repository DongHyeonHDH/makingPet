package com.gorapaduk.makingpet.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.Rarity;
import com.gorapaduk.makingpet.common.dto.PageResponse;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.item.dto.ItemSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import tools.jackson.databind.json.JsonMapper;

/** Redis 없이 캐시 설정(키 접두사, 값 직렬화)만 검증한다. 실제 Redis 연동은 ItemCatalogRedisCacheTest. */
class CacheConfigTest {

    private RedisCacheConfiguration itemCatalogConfig() {
        var builder = RedisCacheManager.builder(new LettuceConnectionFactory());   // 연결하지 않는다
        new CacheConfig().redisCacheManagerBuilderCustomizer(JsonMapper.builder().build()).customize(builder);
        return builder.getCacheConfigurationFor(CacheConfig.ITEM_CATALOG).orElseThrow();
    }

    @Test
    @DisplayName("itemCatalog 캐시 키는 makingpet:cache:itemCatalog:: 로 시작한다")
    void keyPrefix() {
        assertThat(itemCatalogConfig().getKeyPrefixFor(CacheConfig.ITEM_CATALOG))
                .isEqualTo("makingpet:cache:itemCatalog::");
    }

    @Test
    @DisplayName("PageResponse<ItemSummary> 는 타입 정보 없는 JSON 으로 저장되고 같은 값으로 복원된다")
    void valueRoundTrip() {
        var page = new PageResponse<>(List.of(
                new ItemSummary(UUID.randomUUID(), "ribbon", "딸기 리본", ItemCategory.ACCESSORY, Rarity.COMMON,
                        "items/ribbon.png", true, null)),
                0, 50, 1, 1, true, true);
        var pair = itemCatalogConfig().getValueSerializationPair();

        ByteBuffer stored = pair.write(page);
        String json = StandardCharsets.UTF_8.decode(stored.duplicate()).toString();
        Object restored = pair.read(stored);

        assertThat(json).contains("\"code\":\"ribbon\"").doesNotContain("@class");
        assertThat(restored).isEqualTo(page);
    }
}
