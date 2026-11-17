package com.gorapaduk.makingpet.reward.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.reward.domain.RewardClaim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardClaimRepository extends JpaRepository<RewardClaim, UUID> {

    Optional<RewardClaim> findByMemberIdAndTypeAndRewardDate(UUID memberId, RewardClaim.Type type, LocalDate date);

    boolean existsByMemberIdAndTypeAndRewardDate(UUID memberId, RewardClaim.Type type, LocalDate date);

    List<RewardClaim> findByMemberIdAndTypeAndRewardDateBetweenOrderByRewardDateAsc(
            UUID memberId, RewardClaim.Type type, LocalDate from, LocalDate to);
}
