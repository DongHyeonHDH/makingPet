package com.gorapaduk.makingpet.common.config;

import java.time.Duration;

import com.gorapaduk.makingpet.common.dto.PageResponse;
import com.gorapaduk.makingpet.item.dto.ItemSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/**
 * 캐시. 저장소는 spring.cache.type 으로 고른다.
 * - none (기본, 로컬·테스트): 캐시하지 않는다. Redis 없이 뜬다.
 * - redis (운영, CACHE_TYPE=redis): 아래 캐시별 설정으로 Redis 에 저장한다.
 * 캐시는 최적화일 뿐이므로 Redis 장애 시에는 경고만 남기고 원본 조회로 넘어간다.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /** I-1 아이템 카탈로그 목록 (PageResponse&lt;ItemSummary&gt;). 등록·수정 시 전체 무효화. */
    public static final String ITEM_CATALOG = "itemCatalog";

    private static final String KEY_PREFIX = "makingpet:cache:";

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
        JavaType itemCatalogType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponse.class, ItemSummary.class);
        return builder -> builder
                .transactionAware()          // put·evict 를 커밋 후로 미룬다. 롤백된 데이터가 캐시에 남지 않게.
                .withCacheConfiguration(ITEM_CATALOG, jsonCache(objectMapper, itemCatalogType, Duration.ofMinutes(10)));
    }

    /** 값 타입을 캐시마다 고정한다. record 는 final 이라 기본 타입 정보(@class) 방식으로는 역직렬화되지 않는다. */
    private static RedisCacheConfiguration jsonCache(ObjectMapper objectMapper, JavaType valueType, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(KEY_PREFIX)
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(objectMapper, valueType)));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("캐시 조회 실패 — 원본 조회로 대체 cache={} key={} cause={}", cache.getName(), key, e.toString());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("캐시 저장 실패 cache={} key={} cause={}", cache.getName(), key, e.toString());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("캐시 삭제 실패 cache={} key={} cause={}", cache.getName(), key, e.toString());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("캐시 비우기 실패 cache={} cause={}", cache.getName(), e.toString());
            }
        };
    }
}
