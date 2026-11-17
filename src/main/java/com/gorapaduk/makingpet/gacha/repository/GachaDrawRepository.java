package com.gorapaduk.makingpet.gacha.repository;

import java.util.UUID;

import com.gorapaduk.makingpet.gacha.domain.GachaDraw;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GachaDrawRepository extends JpaRepository<GachaDraw, UUID> {
    Page<GachaDraw> findByMemberId(UUID memberId, Pageable pageable);
    Page<GachaDraw> findByMemberIdAndPoolId(UUID memberId, UUID poolId, Pageable pageable);
}
