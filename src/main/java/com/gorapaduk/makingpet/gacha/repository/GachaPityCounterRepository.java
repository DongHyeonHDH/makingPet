package com.gorapaduk.makingpet.gacha.repository;

import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.gacha.domain.GachaPityCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GachaPityCounterRepository extends JpaRepository<GachaPityCounter, GachaPityCounter.Key> {

    /** 같은 회원이 동시에 뽑으면 카운터 행에서 직렬화된다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from GachaPityCounter c where c.id.memberId = :memberId and c.id.poolId = :poolId")
    Optional<GachaPityCounter> findForUpdate(@Param("memberId") UUID memberId, @Param("poolId") UUID poolId);
}
