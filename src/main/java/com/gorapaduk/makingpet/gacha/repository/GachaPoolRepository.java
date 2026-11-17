package com.gorapaduk.makingpet.gacha.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.gacha.domain.GachaPool;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GachaPoolRepository extends JpaRepository<GachaPool, UUID> {
    List<GachaPool> findByActiveTrueOrderByCreatedAtAsc();
    Optional<GachaPool> findByName(String name);
}
