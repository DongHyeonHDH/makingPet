package com.gorapaduk.makingpet.home.repository;

import java.time.LocalDate;
import java.util.UUID;

import com.gorapaduk.makingpet.home.domain.HomeLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeLikeRepository extends JpaRepository<HomeLike, UUID> {

    boolean existsByLikerIdAndOwnerIdAndLikeDate(UUID likerId, UUID ownerId, LocalDate likeDate);
}
