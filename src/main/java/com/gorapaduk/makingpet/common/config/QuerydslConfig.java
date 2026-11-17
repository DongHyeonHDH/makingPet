package com.gorapaduk.makingpet.common.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL. 조건이 동적으로 붙는 조회(카탈로그 필터, M2 경매장 검색)에서 쓴다.
 * 사용처는 `{Domain}RepositoryCustom` + `{Domain}RepositoryImpl` 로 두고 Spring Data 리포지토리에 합친다.
 */
@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
