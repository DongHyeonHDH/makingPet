package com.gorapaduk.makingpet.home.repository;

import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.home.domain.Home;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeRepository extends JpaRepository<Home, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Home h where h.memberId = :memberId")
    Optional<Home> findForUpdate(@Param("memberId") UUID memberId);
}
