package com.gorapaduk.makingpet.home.repository;

import java.util.List;
import java.util.UUID;

import com.gorapaduk.makingpet.home.domain.HomeFurniture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeFurnitureRepository extends JpaRepository<HomeFurniture, UUID> {
    List<HomeFurniture> findByMemberIdOrderByLayerAsc(UUID memberId);
    void deleteByMemberId(UUID memberId);
}
