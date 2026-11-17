package com.gorapaduk.makingpet.reward.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.reward.domain.DailyCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyCounterRepository extends JpaRepository<DailyCounter, DailyCounter.Key> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from DailyCounter c where c.id.memberId = :memberId and c.id.date = :date and c.id.type = :type")
    Optional<DailyCounter> findForUpdate(@Param("memberId") UUID memberId, @Param("date") LocalDate date, @Param("type") String type);
}
