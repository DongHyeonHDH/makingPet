package com.gorapaduk.makingpet.pet.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.pet.domain.UserPet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPetRepository extends JpaRepository<UserPet, UUID> {

    List<UserPet> findByMemberIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID memberId);

    Optional<UserPet> findByIdAndDeletedAtIsNull(UUID id);

    /** 상호작용·장착처럼 상태를 바꾸는 경로는 펫 행을 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPet p where p.id = :id and p.deletedAt is null")
    Optional<UserPet> findForUpdate(@Param("id") UUID id);

    boolean existsByMemberIdAndSpeciesId(UUID memberId, UUID speciesId);

    long countByMemberIdAndDeletedAtIsNull(UUID memberId);

    @Query("select p.speciesId from UserPet p where p.memberId = :memberId and p.deletedAt is null")
    List<UUID> findOwnedSpeciesIds(@Param("memberId") UUID memberId);
}
